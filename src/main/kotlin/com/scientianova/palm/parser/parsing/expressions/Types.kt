package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.inIdent
import com.scientianova.palm.lexer.outIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseAnnotation
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.recBuildList

fun Parser.parseType(): PType? {
    val normalType: PType = when (val token = current) {
        is Token.Ident -> parseNamedType()
        is Token.Parens -> parseTypeTuple(token.tokens)
        is Token.Brackets -> parseTypeList(token.tokens)
        Token.At -> return parseAnnotatedType()
        else -> return null
    }
    return parseTypeNullability(normalType)
}

fun Parser.requireType() = parseType() ?: run {
    err("Missing type")
    emptyType.let { if (current == Token.End) it.noPos() else it.end() }
}

fun Parser.parseTypeAnn() = if (current == Token.Colon) {
    advance().requireType()
} else {
    null
}

fun Parser.parseEqType() = if (current == Token.Assign) {
    advance().requireType()
} else {
    null
}

fun Parser.requireEqType() = if (current == Token.Assign) {
    advance().requireType()
} else {
    err("Missing type")
    emptyType.let { if (current == Token.End) it.noPos() else it.end() }
}

fun Parser.requireTypeAnn() = parseTypeAnn() ?: run {
    err("Missing type annotation")
    emptyType.let { if (current == Token.End) it.noPos() else it.end() }
}

private fun Parser.parseAnnotatedType(): PType? = withPos { start ->
    val annotation = parseAnnotation() ?: return advance().parseType()
    val type = requireType()
    Type.Annotated(annotation, type).at(start, type.next)
}

private fun Parser.parseTypePath(): List<PString> = recBuildList {
    add(parseIdent())

    if (current == Token.Dot) {
        advance()
    } else {
        return this
    }
}

fun Parser.parseNestedTypeArgs(): List<Arg<PNestedType>> = recBuildList {
    val typeStart = current
    if (typeStart == Token.Greater)
        return this

    add(
        if (typeStart is Token.Ident && next == Token.Colon) {
            val ident = typeStart.name.end()
            val type = advance().parseNestedType()
            Arg(ident, type)
        } else {
            Arg(null, parseNestedType())
        }
    )

    when (current) {
        Token.Comma -> advance()
        Token.Greater -> return this
        else -> err("Unclosed angle brackets")
    }
}

fun Parser.parseTypeArgs(): List<Arg<PType>> = recBuildList {
    val typeStart = current
    if (typeStart == Token.Greater)
        return this

    add(
        if (typeStart is Token.Ident && next == Token.Colon) {
            val ident = typeStart.name.end()
            val type = advance().requireType()
            Arg(ident, type)
        } else {
            Arg(null, requireType())
        }
    )

    when (current) {
        Token.Comma -> advance()
        Token.Greater -> return this
        else -> err("Unclosed angle brackets")
    }
}

private fun Parser.parseNamedType(): PType = withPos { start ->
    val path = parseTypePath()
    val next: StringPos
    val args = if (current is Token.Less) {
        advance().parseNestedTypeArgs().also {
            next = nextPos
            advance()
        }
    } else {
        next = path.last().next
        emptyList()
    }

    return Type.Named(path, args).at(start, next)
}

private fun Parser.parseTypeNullability(type: PType): PType =
    if (current == Token.QuestionMark && currentPostfix()) Type.Nullable(type).at(type.start, pos).also { advance() } else type

private fun Parser.parseTypeTupleBody(list: MutableList<PType> = mutableListOf()): List<PType> = recBuildList(list) {
    if (current == Token.End) {
        return this
    } else {
        add(requireType())

        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Missing comma")
        }
    }
}

private fun Parser.parseTypeTuple(tokens: List<PToken>): PType = withPos { start ->
    val list = parenthesizedOf(tokens).parseTypeTupleBody()
    val afterTokens = nextPos
    return if (advance().current == Token.Arrow) {
        val returnType = advance().requireType()
        Type.Function(emptyList(), list, returnType).at(start, returnType.next)
    } else Type.Tuple(list).at(start, afterTokens)
}

private fun Parser.parseTypeList(tokens: List<PToken>): PType = with(parenthesizedOf(tokens)) {
    when (current) {
        Token.End -> return parseFunType(emptyList())
        else -> {
            val firstType = requireType()
            when (current) {
                Token.Comma -> return parseFunType(parseTypeTupleBody(mutableListOf(firstType)))
                Token.Colon -> Type.Dict(firstType, advance().requireType())
                Token.End -> Type.Lis(firstType)
                else -> {
                    err("Missing comma")
                    return parseFunType(parseTypeTupleBody(mutableListOf(firstType)))
                }
            }
        }
    }
}.end()

private fun Parser.parseFunType(context: List<PType>): PType = withPos { start ->
    val params = advance().inParensOr(Parser::parseTypeTupleBody) {
        err("Missing function parameters")
        emptyList()
    }
    if (current == Token.Arrow) advance() else err("Missing arrow")
    val returnType = requireType()
    Type.Function(context, params, returnType).at(start, returnType.next)
}

private fun Parser.parseNestedType(): PNestedType = when (current) {
    Token.Times -> NestedType.Star.end()
    inIdent -> parseNormalNestedType(VarianceMod.In)
    outIdent -> parseNormalNestedType(VarianceMod.Out)
    else -> {
        val type = requireType()
        NestedType.Normal(type, VarianceMod.None).at(type.start, type.next)
    }
}

private fun Parser.parseNormalNestedType(variance: VarianceMod): PNestedType =
    NestedType.Normal(advance().requireType(), variance).end(pos)