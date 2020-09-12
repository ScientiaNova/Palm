package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.*
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.identTokens
import com.scientianova.palm.lexer.isIdentifier
import com.scientianova.palm.lexer.prefixTokens
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map
import java.util.*

fun parseTerm(parser: Parser): PExpr? = when (val token = parser.current) {
    in identTokens -> {
        if (parser.rawLookup(1) == Token.At) {
            val startMark = parser.Marker()
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
    is Token.Bool -> parser.advance().end(Expr.Bool(token.value))
    is Token.Char -> parser.advance().end(Expr.Char(token.value))
    is Token.Str -> parser.advance().end(Expr.Str(token.parts))
    Token.Null -> parser.advance().end(Expr.Null)
    Token.This -> parseThis(parser)
    Token.Super -> parseSuper(parser)
    Token.Return -> parseReturn(parser)
    Token.Break -> parseBreak(parser)
    Token.Continue -> parser.advance().end(Expr.Continue(parseLabelRef(parser.advance())))
    Token.If -> parseIf(parser)
    Token.When -> parseWhen(parser)
    Token.For -> parseFor(parser, null)
    Token.While -> parseWhile(parser, null)
    Token.Loop -> parseLoop(parser, null)
    Token.LBrace -> parseLambda(parser, null)
    Token.LBracket -> parseListOrMap(parser)
    Token.LParen -> parseTuple(parser)
    Token.Object -> TODO()
    Token.Spread -> parser.advance().end(Expr.Spread(requireSubExpr(parser.advance())))
    Token.DoubleColon -> parseFreeFunRef(parser)
    in prefixTokens ->
        parser.Marker().end(Expr.Unary(token.unaryOp(), requireSubExpr(parser.advance())))
    else -> null
}

fun parseLabeledTerm(parser: Parser, label: PString): PExpr = when (parser.current) {
    Token.For -> parseFor(parser, label)
    Token.While -> parseWhile(parser, label)
    Token.Loop -> parseLoop(parser, label)
    Token.LBrace -> parseLambda(parser, label)
    else -> parser.err(missingExpression)
}

fun parseFreeFunRef(parser: Parser): PExpr {
    val startMark = parser.Marker()
    parser.advance()
    val ident = parser.current
    return if (ident.isIdentifier()) {
        parser.advance()
        startMark.end(Expr.FunRef(null, parser.end(ident.toString())))
    } else {
        parser.err(invalidFunctionReference)
    }
}

fun parseThis(parser: Parser): PExpr {
    val start = parser.Marker()
    val label = parseLabelRef(parser.advance())
    return start.end(Expr.This(label))
}

fun parseSuper(parser: Parser): PExpr {
    val start = parser.Marker()
    val label = parseLabelRef(parser.advance())
    return start.end(Expr.This(label))
}

fun parseReturn(parser: Parser): PExpr {
    val start = parser.Marker()
    val label = parseLabelRef(parser.advance())
    val expr = parseBinOps(parser)
    return start.end(Expr.Return(label, expr))
}

fun parseBreak(parser: Parser): PExpr {
    val start = parser.Marker()
    val label = parseLabelRef(parser.advance())
    val expr = parseBinOps(parser)
    return start.end(Expr.Break(label, expr))
}

fun parseLabelRef(parser: Parser): PString? = if (parser.current == Token.At) {
    val start = parser.Marker()
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

tailrec fun parsePostfix(parser: Parser, term: PExpr): PExpr = when (val token = parser.current) {
    Token.LBrace -> if (parser.excludeCurly) {
        term
    } else {
        parsePostfix(parser, Expr.Call(term, CallArgs(last = parseLambda(parser, null))).at(term.start, parser.pos))
    }
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
    in identTokens -> if (parser.rawLookup(1) != Token.At || parser.lastNewline) {
        term
    } else {
        val lStart = parser.Marker()
        parser.advance().advance()
        val label = lStart.end(token.identString())

        if (parser.current != Token.LBrace) parser.err(missingLambda)
        parsePostfix(parser, Expr.Call(term, CallArgs(last = parseLambda(parser, label))).at(term.start, parser.pos))
    }
    Token.Dot -> {
        val operand = parser.advance().current
        val newTerm = when {
            operand.isIdentifier() ->
                Expr.MemberAccess(term, parser.advance().end(operand.identString())).at(term.start, parser.pos)
            operand is Token.Int ->
                Expr.ComponentAccess(term, parser.advance().end(operand.value)).at(term.start, parser.pos)
            else -> parser.err(invalidAccessOperand)
        }
        parsePostfix(parser, newTerm)
    }
    Token.SafeAccess -> {
        val operand = parser.advance().current
        val newTerm = when {
            operand.isIdentifier() ->
                Expr.SafeMemberAccess(term, parser.advance().end(operand.identString())).at(term.start, parser.pos)
            operand is Token.Int ->
                Expr.SafeComponentAccess(term, parser.advance().end(operand.value)).at(term.start, parser.pos)
            else -> parser.err(invalidAccessOperand)
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
    Token.RangeFrom -> parsePostfix(
        parser.advance(),
        Expr.Unary(UnaryOp.RangeFrom, term).at(term.start, parser.pos)
    )
    else -> term
}

fun parseSubExpr(parser: Parser) = parseTerm(parser)?.let { parsePostfix(parser, it) }

fun requireSubExpr(parser: Parser) = parseSubExpr(parser) ?: parser.err(missingExpression)

fun parseBinOpsBody(parser: Parser, opStack: Stack<Token>, operandStack: Stack<PExpr>): PExpr {
    while (true) {
        val op = parser.current
        val opPrecedence = op.precedence

        while (opStack.peek().precedence >= opPrecedence) {
            operandStack.push(opStack.pop().handleBinary(operandStack.pop(), operandStack.pop()))
        }

        if (opPrecedence < 0) {
            return operandStack.pop()
        }

        op.handleBinaryAppend(parser.advance(), opStack, operandStack)
    }
}

fun parseBinOps(parser: Parser): PExpr? = parseTerm(parser)?.let { term ->
    parseBinOps(parser, parsePostfix(parser, term))
}

fun parseBinOps(parser: Parser, first: PExpr): PExpr = if (parser.current.precedence > 0) {
    val opStack = Stack<Token>()
    opStack.push(Token.PlaceHolder)

    val operandStack = Stack<PExpr>()
    operandStack.push(first)

    parseBinOpsBody(parser, opStack, operandStack)
} else {
    first
}

fun requireBinOps(parser: Parser) = parseBinOps(parser) ?: parser.err(missingExpression)

fun parseEqExpr(parser: Parser): PExpr? = if (parser.current == Token.Assign) {
    requireBinOps(parser.advance())
} else {
    null
}

private fun parseTupleBody(parser: Parser): List<PExpr> = recBuildList<PExpr> {
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

fun parseTuple(parser: Parser): PExpr {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val list = parseTupleBody(parser)

    parser.advance()
    marker.revertFlags()

    return if (list.size == 1) {
        list[0]
    } else {
        marker.end(Expr.Tuple(list))
    }
}

fun parseScopeExpr(parser: Parser): PExpr {
    val start = parser.Marker()

    return start.end(Expr.Scope(parseScopeBody(parser)))
}

private fun parseWhenBranch(parser: Parser): WhenBranch {
    val pattern = parsePattern(parser, DecHandling.None)
    if (parser.current != Token.Arrow) parser.err(missingArrowOnBranch)
    parser.advance()
    val expr = if (parser.current == Token.LBrace) {
        parseScopeExpr(parser)
    } else {
        requireBinOps(parser)
    }

    return pattern to expr
}

private fun parseWhenBranches(parser: Parser): List<WhenBranch> = recBuildList<WhenBranch> {
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

fun parseWhen(parser: Parser): PExpr {
    val marker = parser.Marker()

    parser.advance()

    val expr = if (parser.current == Token.LBrace) {
        null
    } else {
        parser.excludeCurly = true
        parser.trackNewline = false
        requireBinOps(parser)
    }

    if (parser.current != Token.LBrace) parser.err(missingScope)
    parser.excludeCurly = false
    parser.trackNewline = true

    val branches = parseWhenBranches(parser.advance())

    parser.advance()
    marker.revertFlags()

    return marker.end(Expr.When(expr, branches))
}


fun parseCondition(parser: Parser): Condition = when (parser.current) {
    Token.Val -> parsePatternCondition(parser, DecHandling.Val)
    Token.Var -> parsePatternCondition(parser, DecHandling.Var)
    else -> Condition.Expr(requireBinOps(parser))
}

fun parsePatternCondition(parser: Parser, handling: DecHandling): Condition {
    val pattern = parsePatternNoExpr(parser.advance(), handling)
    val expr = parseEqExpr(parser) ?: parser.err(missingExpression)
    return Condition.Pattern(pattern, expr)
}

fun parseConditions(parser: Parser): List<Condition> {
    val marker = parser.Marker()

    parser.trackNewline = false
    parser.excludeCurly = true

    recBuildList<Condition> {
        add(parseCondition(parser))
        if (parser.current != Token.Comma) {
            marker.revertFlags()
            return this
        }
    }
}

fun parseIf(parser: Parser): PExpr {
    val start = parser.Marker()
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

fun parseWhile(parser: Parser, label: PString?): PExpr {
    val start = parser.Marker()

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

fun parseFor(parser: Parser, label: PString?): PExpr {
    val start = parser.Marker()

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

fun parseLoop(parser: Parser, label: PString?): PExpr {
    val start = parser.Marker()

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

private fun parseMapBody(parser: Parser, list: MutableList<Pair<PExpr, PExpr>>): List<Pair<PExpr, PExpr>> {
    recBuildList(list) {
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
}

fun parseListOrMap(parser: Parser): PExpr {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val expr = when (parser.current) {
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

    parser.advance()
    marker.revertFlags()

    return marker.end(expr)
}

fun parseGet(parser: Parser, on: PExpr): PExpr {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val list = parseListBody(parser, mutableListOf())

    parser.advance()
    marker.revertFlags()

    return if (list.isEmpty()) {
        marker.err(emptyGetBody)
    } else {
        Expr.Get(on, list).at(on.start, parser.pos)
    }
}

fun parseLambdaParams(parser: Parser): LambdaParams = recBuildList<Pair<PDecPattern, PType?>> {
    val pattern = parseDecPattern(parser) ?: return emptyList()
    val type = parseTypeAnn(parser)
    add(pattern to type)
    when (parser.current) {
        Token.Comma -> parser.advance()
        Token.Arrow -> return this
        else -> return emptyList()
    }
}

fun parseLambda(parser: Parser, label: PString?): PExpr {
    val start = parser.Marker()
    parser.advance()

    val params = parseLambdaParams(parser)

    if (params.isEmpty()) {
        start.revertIndex()
    }

    val body = parseScopeBody(parser)

    return start.end(Expr.Lambda(label, params, body))
}

private fun parseCallBody(parser: Parser): List<Arg> = recBuildList<Arg> {
    val current = parser.current
    if (current == Token.RParen) {
        return this
    } else {
        if (current.isIdentifier()) {
            val ident = parser.advance().end(current.identString())
            val expr = parseEqExpr(parser)

            add(
                if (expr == null) {
                    Arg.Free(parseBinOps(parser, parsePostfix(parser, ident.map(Expr::Ident))))
                } else {
                    Arg.Named(ident, expr)
                }
            )
        } else {
            add(Arg.Free(requireBinOps(parser)))
        }

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

fun parseCall(parser: Parser, on: PExpr): PExpr {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val list = parseCallBody(parser)

    parser.advance()
    marker.revertFlags()

    val last = if (parser.excludeCurly) {
        null
    } else {
        when (val token = parser.current) {
            Token.LBrace -> parseLambda(parser, null)
            in identTokens -> if (parser.rawLookup(1) == Token.At) {
                val lStart = parser.Marker()
                parser.advance().advance()
                val label = lStart.end(token.identString())
                if (parser.current != Token.LBrace) parser.err(missingLambda)
                parseLambda(parser, label)
            } else {
                null
            }
            else -> null
        }
    }

    return Expr.Call(on, CallArgs(list, last)).at(on.start, parser.pos)
}