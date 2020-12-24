package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.missingPackage
import com.scientianova.palm.errors.unclosedSquareBracket
import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireEqExpr
import com.scientianova.palm.parser.parsing.expressions.requireEqType
import com.scientianova.palm.parser.parsing.types.*
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString

fun parseFile(parser: Parser): FileScope {
    val metadataComments = parseMetadataComments(parser)
    val annotations = parseAnnotations(parser)

    val pack = if (parser.current == Token.Package) {
        parsePackage(parser.advance())
    } else {
        parser.err(missingPackage)
    }

    val imports = parseImports(parser)
    val statements = parseStatements(parser)

    return FileScope(metadataComments, annotations, pack, imports, statements)
}

private fun parsePackage(parser: Parser) = recBuildList<PString> {
    add(parseIdent(parser))
    if (parser.current == Token.Dot) {
        parser.advance()
    } else {
        return this
    }
}

private fun parseMetadataComments(parser: Parser) = recBuildList<PString> {
    val current = parser.current
    if (current is Token.MetadataComment) {
        add(parser.advance().end(current.content))
    } else {
        return this
    }
}

private fun parseStatements(parser: Parser) = recBuildList<FileStmt> {
    when (parser.current) {
        Token.EOF -> return this
        Token.Semicolon -> parser.advance()
        Token.Extend -> add(StaticExtension(parseExtension(parser.advance())))
        Token.Impl -> add(StaticImpl(parseImpl(parser.advance())))
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> StaticProperty(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> StaticProperty(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> StaticFunction(parseFun(parser.advance(), modifiers))
                    Token.Object -> StaticObject(parseObject(parser.advance(), modifiers))
                    Token.Trait -> StaticTypeClass(parseTrait(parser.advance(), modifiers))
                    Token.Record -> StaticRecord(parseRecord(parser.advance(), modifiers))
                    Token.Enum -> StaticEnum(parseEnum(parser.advance(), modifiers))
                    Token.Class -> StaticClass(parseClass(parser.advance(), modifiers))
                    Token.Mixin -> StaticMixin(parseMixin(parser.advance(), modifiers))
                    Token.Const -> parseConst(parser.advance(), modifiers)
                    Token.Type -> parseTpeAlias(parser.advance(), modifiers)
                    else -> parser.err(unexpectedMember("file"))
                }
            )
        }
    }
}

private fun parseConst(parser: Parser, modifiers: List<DecModifier>): FileStmt {
    val name = parseIdent(parser)
    val type = parseTypeAnn(parser)
    val expr = requireEqExpr(parser)

    return Constant(name, modifiers, type, expr)
}

private fun parseTpeAlias(parser: Parser, modifiers: List<DecModifier>): FileStmt {
    val name = parseIdent(parser)
    val params = if (parser.current == Token.LBracket) {
        parseAliasParams(parser.advance())
    } else {
        emptyList()
    }

    val actual = requireEqType(parser)

    return TypeAlias(name, modifiers, params, actual)
}

private fun parseAliasParams(parser: Parser) = recBuildList<PString> {
    if (parser.current == Token.RBracket) {
        parser.advance()
        return this
    }

    add(parseIdent(parser))

    when (parser.current) {
        Token.Comma -> parser.advance()
        Token.RBracket -> {
            parser.advance()
            return this
        }
        else -> parser.err(unclosedSquareBracket)
    }
}