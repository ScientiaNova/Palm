package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.*
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.identTokens
import com.scientianova.palm.lexer.isIdentifier
import com.scientianova.palm.lexer.prefixTokens
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parsing.top.parseAnnotation
import com.scientianova.palm.parser.parsing.types.parseObjectBody
import com.scientianova.palm.parser.parsing.types.parseSuperTypes
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at

private fun parseTerm(parser: Parser): PExpr? = when (val token = parser.current) {
    in identTokens -> {
        if (parser.rawLookup(1) == Token.At) {
            val startMark = parser.mark()
            parseLabeledTerm(parser.advance().advance(), startMark.end(token.identString()))
        }
        parser.advance().end(Expr.Ident(token.identString()))
    }
    is Token.Byte -> parser.advance().end(Expr.Byte(token.value))
    is Token.Short -> parser.advance().end(Expr.Short(token.value))
    is Token.Int -> parser.advance().end(Expr.Int(token.value))
    is Token.Long -> parser.advance().end(Expr.Long(token.value))
    is Token.Float -> parser.advance().end(Expr.Float(token.value))
    is Token.Double -> parser.advance().end(Expr.Double(token.value))
    is Token.BoolLit -> parser.advance().end(Expr.Bool(token.value))
    is Token.CharLit -> parser.advance().end(Expr.Char(token.value))
    is Token.StrLit -> parser.advance().end(Expr.Str(token.parts))
    Token.NullLit -> parser.advance().end(Expr.Null)
    Token.Wildcard -> parser.advance().end(Expr.Wildcard)
    Token.This -> parser.advance().end(Expr.This)
    Token.Super -> parser.advance().end(Expr.Super)
    Token.Return -> parseReturn(parser)
    Token.Break -> parseBreak(parser)
    Token.Continue -> parser.advance().end(Expr.Continue(parseLabelRef(parser.advance())))
    Token.If -> parseIf(parser)
    Token.When -> parseWhen(parser)
    Token.For -> parseFor(parser, null)
    Token.While -> parseWhile(parser, null)
    Token.Loop -> parseLoop(parser, null)
    Token.Throw -> parser.mark().end(Expr.Throw(requireBinOps(parser.advance())))
    Token.Do -> parser.mark().end(Expr.Do(requireScope(parser), parseCatches(parser)))
    Token.LBrace -> parseLambda(parser, null)
    Token.LBracket -> parseListOrMap(parser)
    Token.LParen -> parseTuple(parser)
    Token.Object -> parseObject(parser)
    Token.Spread -> parser.advance().end(Expr.Spread(requireSubExpr(parser.advance())))
    Token.DoubleColon -> parseFreeFunRef(parser)
    Token.At -> parser.mark().end(Expr.Annotated(parseAnnotation(parser), requireTerm(parser)))
    Token.Defer -> parser.mark().end(Expr.Defer(requireScope(parser.advance())))
    Token.Guard -> parseGuard(parser)
    Token.Val -> parseDec(parser, false)
    Token.Var -> parseDec(parser, true)
    in prefixTokens ->
        parser.mark().end(Expr.Unary(token.unaryOp(), requireSubExpr(parser.advance())))
    else -> null
}

private fun requireTerm(parser: Parser) = parseTerm(parser) ?: parser.err(missingExpression)

private fun parseLabeledTerm(parser: Parser, label: PString): PExpr = when (parser.current) {
    Token.For -> parseFor(parser, label)
    Token.While -> parseWhile(parser, label)
    Token.Loop -> parseLoop(parser, label)
    Token.LBrace -> parseLambda(parser, label)
    else -> parser.err(missingExpression)
}

private fun parseFreeFunRef(parser: Parser): PExpr {
    val startMark = parser.mark()
    parser.advance()
    val ident = parser.current
    return if (ident.isIdentifier()) {
        parser.advance()
        startMark.end(Expr.FunRef(null, parser.end(ident.toString())))
    } else {
        parser.err(invalidFunctionReference)
    }
}

private fun parseReturn(parser: Parser): PExpr {
    val start = parser.mark()
    val label = parseLabelRef(parser.advance())
    val expr = parseBinOps(parser)
    return start.end(Expr.Return(label, expr))
}

private fun parseBreak(parser: Parser): PExpr {
    val start = parser.mark()
    val label = parseLabelRef(parser.advance())
    val expr = parseBinOps(parser)
    return start.end(Expr.Break(label, expr))
}

private fun parseLabelRef(parser: Parser): PString? = if (parser.current == Token.At) {
    val start = parser.mark()
    val ident = parser.rawLookup(1)
    if (ident.isIdentifier()) {
        parser.advance().advance()
        start.end(ident.identString())
    } else {
        parser.advance().err(missingLabelName)
    }
} else {
    null
}

private fun parseCatches(parser: Parser): List<Catch> = recBuildList {
    if (parser.current == Token.Catch) {
        val pattern = requireDecPattern(parser.advance())
        val type = requireTypeAnn(parser)
        val scope = requireScope(parser)
        add(Catch(pattern, type, scope))
    } else {
        return this
    }
}

private tailrec fun parsePostfix(parser: Parser, term: PExpr): PExpr = when (parser.current) {
    Token.LBracket -> if (parser.lastNewline) {
        term
    } else {
        parsePostfix(parser, parseGet(parser, term))
    }
    Token.LParen -> if (parser.lastNewline) {
        term
    } else {
        parsePostfix(parser, parseCall(parser, term))
    }
    Token.Dot -> {
        val operand = parser.advance().current
        val newTerm = when {
            operand.isIdentifier() ->
                Expr.MemberAccess(term, parser.advance().end(operand.identString()))
            operand == Token.LBracket ->
                Expr.Turbofish(term, parseTypeArgs(parser.advance()))
            else -> parser.err(invalidAccessOperand)
        }
        parsePostfix(parser, newTerm.at(term.start, parser.pos))
    }
    Token.SafeAccess -> {
        val operand = parser.advance().current
        val newTerm = if (operand.isIdentifier()) {
            Expr.SafeMemberAccess(term, parser.advance().end(operand.identString())).at(term.start, parser.pos)
        } else {
            parser.err(invalidAccessOperand)
        }
        parsePostfix(parser, newTerm)
    }
    Token.DoubleColon -> {
        parser.advance()
        val ident = parser.current
        if (ident.isIdentifier()) {
            parsePostfix(
                parser.advance(),
                Expr.FunRef(term, parser.end(ident.identString())).at(term.start, parser.pos)
            )
        } else {
            parser.err(invalidFunctionReference)
        }
    }
    Token.NonNull -> parsePostfix(parser.advance(), Expr.Unary(UnaryOp.NonNull, term).at(term.start, parser.pos))
    else -> if (parser.excludeCurly) {
        term
    } else {
        val marker = parser.mark()
        val lambda = parsePostfixLambda(parser)

        if (lambda == null) {
            term
        } else {
            parsePostfix(parser, marker.end(Expr.Call(term, CallArgs(last = lambda))))
        }
    }
}

fun parsePostfixLambda(parser: Parser): PExpr? {
    val current = parser.current
    return when {
        current == Token.At -> {
            val marker = parser.mark()
            val annotation = parseAnnotation(parser)
            val expr = parsePostfixLambda(parser) ?: return null
            marker.end(Expr.Annotated(annotation, expr))
        }
        current.isIdentifier() && parser.rawLookup(1) == Token.At -> {
            val marker = parser.mark()
            parser.advance()
            parser.advance()

            val label = marker.end(current.identString())

            if (parser.current != Token.LBrace) {
                parser.err(missingLambda)
            }

            parseLambda(parser, label)
        }
        current == Token.LBrace -> parseLambda(parser, null)
        else -> null
    }
}

fun parseSubExpr(parser: Parser) = parseTerm(parser)?.let { parsePostfix(parser, it) }
fun requireSubExpr(parser: Parser) = parseSubExpr(parser) ?: parser.err(missingExpression)

fun parseBinOps(parser: Parser): PExpr? = parseSubExpr(parser)?.let { first ->
    parseBinOps(parser, first, 0)
}

tailrec fun parseBinOps(parser: Parser, left: PExpr, minPrecedence: Int): PExpr {
    val current = parser.current
    return if (current.precedence >= minPrecedence) {
        parser.advance()
        parseBinOps(parser, current.handleBinary(parser, left, minPrecedence), minPrecedence)
    } else left
}

fun requireBinOps(parser: Parser) = parseBinOps(parser) ?: parser.err(missingExpression)

fun parseEqExpr(parser: Parser) = if (parser.current == Token.Assign) {
    requireBinOps(parser.advance())
} else {
    null
}

fun requireEqExpr(parser: Parser) = if (parser.current == Token.Assign) {
    requireBinOps(parser.advance())
} else {
    parser.err(missingExpression)
}

private fun parseTupleBody(parser: Parser): List<PExpr> = recBuildList {
    if (parser.current == Token.RParen) {
        return this
    } else {
        add(requireBinOps(parser))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

private fun parseTuple(parser: Parser): PExpr {
    val marker = parser.mark()

    val list = parser.withFlags(trackNewline = false, excludeCurly = false) {
        parseTupleBody(parser.advance())
    }

    parser.advance()

    return if (list.size == 1) {
        list[0]
    } else {
        marker.end(Expr.Tuple(list))
    }
}

fun parseScopeExpr(parser: Parser) = parser.mark().end(Expr.Scope(parseScopeBody(parser)))

private fun parseWhenBranch(parser: Parser): WhenBranch {
    val pattern = parsePattern(parser)

    if (parser.current != Token.Arrow) {
        parser.err(missingArrowOnBranch)
    }

    val expr = if (parser.advance().current == Token.LBrace) {
        parseScopeExpr(parser)
    } else {
        requireBinOps(parser)
    }

    return pattern to expr
}

private fun parseWhenBranches(parser: Parser): List<WhenBranch> = recBuildList {
    when (parser.current) {
        Token.RBrace -> return this
        Token.Semicolon -> parser.advance()
        else -> {
            add(parseWhenBranch(parser))
            val sep = parser.current
            when {
                sep == Token.Semicolon -> parser.advance()
                sep == Token.RBrace -> return this
                parser.lastNewline -> {

                }
                else -> parser.err(unclosedWhen)
            }
        }
    }
}

private fun parseWhen(parser: Parser): PExpr {
    val marker = parser.mark()

    parser.advance()

    val expr = if (parser.current == Token.LBrace) {
        null
    } else {
        parser.withFlags(trackNewline = false, excludeCurly = true) {
            requireBinOps(parser)
        }
    }

    if (parser.current != Token.LBrace) {
        parser.err(missingScope)
    }

    val branches = parser.withFlags(trackNewline = true, excludeCurly = false) {
        parseWhenBranches(parser.advance())
    }

    parser.advance()

    return marker.end(Expr.When(expr, branches))
}


private fun parseCondition(parser: Parser): Condition = when (parser.current) {
    Token.Val -> parsePatternCondition(parser.advance(), false)
    Token.Var -> parsePatternCondition(parser.advance(), true)
    else -> Condition.Expr(requireBinOps(parser))
}

private fun parsePatternCondition(parser: Parser, mutable: Boolean): Condition {
    val pattern = requireSubExpr(parser)
    val expr = parseEqExpr(parser) ?: parser.err(missingExpression)
    return Condition.Pattern(mutable, pattern, expr)
}

fun parseConditions(parser: Parser): List<Condition> = parser.withFlags(trackNewline = false, excludeCurly = true) {
    recBuildList {
        add(parseCondition(parser))
        if (parser.current != Token.Comma) {
            return@withFlags this
        } else Unit
    }
}

private fun parseIf(parser: Parser): PExpr {
    val start = parser.mark()
    parser.advance()

    val conditions = parseConditions(parser)
    val ifScope = requireScope(parser)
    val elseScope = if (parser.current == Token.Else) {
        requireScope(parser.advance())
    } else {
        null
    }

    return start.end(Expr.If(conditions, ifScope, elseScope))
}

private fun parseWhile(parser: Parser, label: PString?): PExpr {
    val start = parser.mark()

    parser.advance()

    val conditions = parseConditions(parser)
    val whileScope = requireScope(parser)
    val noBreakScope = if (parser.current == Token.Nobreak) {
        requireScope(parser.advance())
    } else {
        null
    }

    return start.end(Expr.While(label, conditions, whileScope, noBreakScope))
}

private fun parseFor(parser: Parser, label: PString?): PExpr {
    val start = parser.mark()

    parser.advance()
    val decPattern = requireDecPattern(parser)
    if (parser.current != Token.In) parser.err(missingInInFor)
    val iterExpr = requireBinOps(parser.advance())
    val forScope = requireScope(parser)
    val noBreakScope = if (parser.current == Token.Nobreak) {
        requireScope(parser.advance())
    } else {
        null
    }

    return start.end(Expr.For(label, decPattern, iterExpr, forScope, noBreakScope))
}

private fun parseLoop(parser: Parser, label: PString?): PExpr {
    val start = parser.mark()

    parser.advance()

    return start.end(Expr.Loop(label, requireScope(parser)))
}

private fun parseListBody(parser: Parser, list: MutableList<PExpr>): List<PExpr> = recBuildList(list) {
    if (parser.current == Token.RBracket) {
        return this
    } else {
        add(requireBinOps(parser))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RBracket -> return this
            else -> parser.err(unclosedSquareBracket)
        }
    }
}

private fun parseMapBody(parser: Parser, list: MutableList<Pair<PExpr, PExpr>>) = recBuildList(list) {
    if (parser.current == Token.RBracket) {
        return this
    } else {
        val first = requireBinOps(parser)
        if (parser.current != Token.Colon) parser.err(missingColonInMap)
        val second = requireBinOps(parser.advance())
        add(first to second)

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RBracket -> return this
            else -> parser.err(unclosedSquareBracket)
        }
    }
}

private fun parseListOrMap(parser: Parser): PExpr {
    val marker = parser.mark()

    parser.advance()

    val expr = parser.withFlags(trackNewline = false, excludeCurly = false) {
        when (parser.current) {
            Token.RBracket -> Expr.Lis(emptyList())
            Token.Colon -> {
                if (parser.advance().current != Token.RBracket) parser.err(unclosedSquareBracket)
                Expr.Map(emptyList())
            }
            else -> {
                val first = requireBinOps(parser)
                when (parser.current) {
                    Token.Comma -> Expr.Lis(parseListBody(parser.advance(), mutableListOf(first)))
                    Token.Colon -> {
                        val second = requireBinOps(parser.advance())
                        when (parser.current) {
                            Token.Comma -> Expr.Map(parseMapBody(parser.advance(), mutableListOf(first to second)))
                            Token.RBracket -> Expr.Map(listOf(first to second))
                            else -> parser.err(unclosedSquareBracket)
                        }
                    }
                    Token.RBracket -> Expr.Lis(listOf(first))
                    else -> parser.err(unclosedSquareBracket)
                }
            }
        }
    }

    parser.advance()

    return marker.end(expr)
}

private fun parseGet(parser: Parser, on: PExpr): PExpr {
    val marker = parser.mark()

    parser.advance()

    val list = parser.withFlags(trackNewline = false, excludeCurly = false) {
        parseListBody(parser, mutableListOf())
    }

    parser.advance()

    return if (list.isEmpty()) {
        marker.err(emptyGetBody)
    } else {
        Expr.Get(on, list).at(on.start, parser.pos)
    }
}

private fun parseLambdaParams(parser: Parser): LambdaParams = recBuildList {
    val pattern = parseDecPattern(parser) ?: return emptyList()
    val type = parseTypeAnn(parser)
    add(pattern to type)
    when (parser.current) {
        Token.Comma -> parser.advance()
        Token.Arrow -> return this
        else -> return emptyList()
    }
}

private fun parseLambda(parser: Parser, label: PString?): PExpr {
    val start = parser.mark()
    parser.advance()

    val params = parseLambdaParams(parser)

    if (params.isEmpty()) {
        start.revertIndex()
    }

    val body = parseScopeBody(parser)

    return start.end(Expr.Lambda(label, params, body))
}

fun parseCallArgs(parser: Parser): List<Arg> = parser.withFlags(trackNewline = false, excludeCurly = false) {
    recBuildList {
        val current = parser.current
        if (current == Token.RParen) {
            parser.advance()
            return@withFlags this
        } else {
            if (current.isIdentifier()) {
                val ident = parser.advance().end(current.identString())
                val expr = parseEqExpr(parser)

                add(
                    if (expr == null) {
                        Arg.Free(parseBinOps(parser, parsePostfix(parser, ident.map(Expr::Ident)), 0))
                    } else {
                        Arg.Named(ident, expr)
                    }
                )
            } else {
                add(Arg.Free(requireBinOps(parser)))
            }

            when (parser.current) {
                Token.Comma -> parser.advance()
                Token.RParen -> {
                    parser.advance()
                    return@withFlags this
                }
                else -> parser.err(unclosedParenthesis)
            }
        }
    }
}

private fun parseCall(parser: Parser, on: PExpr): PExpr {
    val list = parseCallArgs(parser.advance())
    val last = if (parser.excludeCurly) {
        null
    } else {
        parsePostfixLambda(parser)
    }

    return Expr.Call(on, CallArgs(list, last)).at(on.start, parser.pos)
}

private fun parseObject(parser: Parser): PExpr {
    val start = parser.mark()

    val superTypes = parseSuperTypes(parser.advance())

    if (parser.current != Token.LBrace) {
        parser.err(missingScope)
    }

    val body = parseObjectBody(parser)

    return start.end(Expr.Object(superTypes, body))
}

private fun parseGuard(parser: Parser): PExpr {
    val start = parser.mark()

    val conditions = parseConditions(parser.advance())
    if (parser.current != Token.Else) parser.err(missingElseInGuard)
    val scope = requireScope(parser.advance())

    return start.end(Expr.Guard(conditions, scope))
}

private fun parseDec(parser: Parser, mutable: Boolean): PExpr {
    val start = parser.mark()

    val pattern = requireSubExpr(parser.advance())
    val type = parseTypeAnn(parser)
    val expr = parseEqExpr(parser)
    return start.end(Expr.Dec(mutable, pattern, type, expr))
}