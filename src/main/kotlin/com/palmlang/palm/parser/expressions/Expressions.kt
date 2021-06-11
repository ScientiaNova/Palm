package com.palmlang.palm.parser.expressions

import com.palmlang.palm.ast.expressions.*
import com.palmlang.palm.errors.PalmError
import com.palmlang.palm.lexer.PToken
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.top.parseStatements
import com.palmlang.palm.parser.top.requireScope
import com.palmlang.palm.util.*

private fun Parser.parseTerm(): PExpr? = when (val token = current) {
    is Token.Ident -> {
        if (rawLookup(1) == Token.At) {
            val startPos = pos
            advance().parseLabeledTerm(token.name.end(startPos))
        }
        Expr.Ident(token.name).end()
    }
    is Token.IntLit -> {
        val start = pos
        val maybeType = rawLookup(1)
        when {
            maybeType is Token.Ident && !maybeType.backticked -> {
                advance()
                when (maybeType.name) {
                    "b", "B" -> Expr.Byte(token.value.toByte()).end(start)
                    "s", "S" -> Expr.Short(token.value.toShort()).end(start)
                    "i", "I" -> Expr.Int(token.value.toInt()).end(start)
                    "l", "L" -> Expr.Long(token.value).end(start)
                    "f", "F" -> Expr.Float(token.value.toFloat()).end(start)
                    "d", "D" -> Expr.Double(token.value.toDouble()).end(start)
                    else -> {
                        err("Unknown literal suffix")
                        Expr.Error.end(start)
                    }
                }
            }
            token.value in Int.MIN_VALUE..Int.MAX_VALUE -> Expr.Int(token.value.toInt()).end(start)
            else -> Expr.Long(token.value).end(start)
        }
    }
    is Token.FloatLit -> {
        val start = pos
        val maybeType = rawLookup(1)
        when {
            maybeType is Token.Ident && !maybeType.backticked -> {
                advance()
                when (maybeType.name) {
                    "f", "F" -> Expr.Float(token.value.toFloat()).end(start)
                    "d", "D" -> Expr.Double(token.value).end(start)
                    else -> {
                        err("Unknown literal suffix")
                        Expr.Error.end(start)
                    }
                }
            }
            else -> Expr.Double(token.value).end(start)
        }
    }
    is Token.BoolLit -> Expr.Bool(token.value).end()
    is Token.CharLit -> Expr.Char(token.value).end()
    is Token.StrLit -> Expr.Str(token.parts.map { it.parse(this) }).end()
    is Token.Braces -> parseLambda(null, token.tokens)
    is Token.Brackets -> parseListOrMap(token.tokens)
    is Token.Parens -> parseParenthesized(token.tokens)
    Token.NullLit -> Expr.Null.end()
    Token.Super -> Expr.Super.end()
    Token.Mod -> Expr.Module.end()
    Token.Return -> parseReturn()
    Token.Break -> parseBreak()
    Token.When -> parseWhen()
    Token.Throw -> withPos { advance().requireExpr().let { expr -> Expr.Throw(expr).at(it, expr.next) } }
    Token.Do -> withPos { advance().requireScope().let { scope -> Expr.Do(scope, parseCatches()).at(it, scope.next) } }
    Token.Spread -> withPos { advance().requireSubExpr().let { expr -> Expr.Spread(expr).at(it, expr.next) } }
    Token.DoubleColon -> parseFreeFunRef()
    Token.Plus -> parsePrefix(UnOp.Not)
    Token.Minus -> parsePrefix(UnOp.Plus)
    Token.ExclamationMark -> parsePrefix(UnOp.Not)
    else -> null
}

fun Parser.parsePrefix(op: UnOp): PExpr? = if (rawLookup(1).canIgnore()) null else withPos {
    val expr = advance().requireSubExpr()
    Expr.Unary(op.at(it), expr).at(it, expr.next)
}

private fun Parser.parseLabeledTerm(label: PString): PExpr = when (val braces = current) {
    is Token.Braces -> parseLambda(label, braces.tokens)
    else -> expectedExpression()
}

private fun Parser.expectedExpression(): PExpr {
    err("Expected expression")
    return Expr.Error.let { if (current == Token.End) it.noPos() else it.end() }
}

private fun Parser.parseFreeFunRef(): PExpr = withPos { start ->
    val ident = parseIdent()
    Expr.FunRef(null, ident).at(start, ident.next)
}

private fun Parser.parseReturn(): PExpr = withPos { start ->
    val label = advance().parseLabelRef()
    val expr = parseBinOpsOnLine()
    Expr.Return(label, expr).at(start, expr?.next ?: label?.next ?: nextPos.also { advance() })
}

private fun Parser.parseBreak(): PExpr = withPos { start ->
    val label = advance().parseLabelRef() ?: run {
        err("Missing label", nextPos, nextPos)
        "".noPos(nextPos).also { advance() }
    }
    val expr = parseBinOpsOnLine()
    Expr.Break(label, expr).at(start, expr?.next ?: label.next)
}

private fun Parser.parseLabelRef(): PString? = if (current == Token.At) {
    val start = pos
    val ident = rawLookup(1)
    if (ident is Token.Ident) {
        advance()
        ident.name.end(start)
    } else {
        err("Missing label name")
        "".end()
    }
} else {
    null
}

private fun Parser.parseCatches(): List<Catch> = recBuildList {
    if (current == Token.Catch) {
        val pattern = advance().requireDecPattern()
        val type = requireTypeAnn()
        val scope = requireScope()
        add(Catch(pattern, type, scope))
    } else {
        return this
    }
}

private tailrec fun Parser.parsePostfix(term: PExpr): PExpr = when (val token = current) {
    is Token.Brackets -> if (lastNewline) term else {
        parsePostfix(parseGet(term, token.tokens))
    }
    is Token.Parens -> if (lastNewline) term else {
        parsePostfix(parseCall(term, token.tokens))
    }
    Token.Dot -> parsePostfix(
        when (val operand = advance().current) {
            is Token.Ident -> {
                val ident = operand.name.end()
                Expr.MemberAccess(term, ident).at(term.start, ident.next)
            }
            is Token.Brackets -> parseContextCall(term, operand.tokens)
            is Token.Less -> parseTurbofish(term)
            else -> {
                err("Invalid member access")
                Expr.Error.end(nextPos)
            }
        }
    )
    Token.QuestionMark -> {
        val startIndex = index
        if (advance().currentPostfix()) when (val afterQ = current) {
            is Token.Dot -> {
                val ident = advance().parseIdent()
                parsePostfix(Expr.Safe(Expr.MemberAccess(term, ident)).at(term.start, ident.next))
            }
            is Token.Brackets -> parsePostfix(parseGet(term, afterQ.tokens).map(Expr::Safe))
            is Token.Parens -> parsePostfix(parseCall(term, afterQ.tokens).map(Expr::Safe))
            else -> {
                index = startIndex
                term
            }
        } else {
            val thenExpr = requireExpr()
            if (current == Token.Colon) advance() else err("Missing colon")
            val elseExpr = requireExpr()
            Expr.Ternary(term, thenExpr, elseExpr).at(term.start, elseExpr.next)
        }
    }
    Token.DoubleColon -> {
        val ident = advance().parseIdent()
        parsePostfix(Expr.FunRef(term, ident).at(term.start, ident.next))
    }
    Token.ExclamationMark -> if (currentPostfix()) {
        parsePostfix(Expr.Unary(UnOp.NonNull.at(pos), term).end(term.start))
    } else {
        term
    }
    else -> {
        val lambdas = parsePostfixLambdas()
        if (lambdas.isEmpty()) {
            term
        } else {
            parsePostfix(
                Expr.Call(term,
                    CallArgs(emptyList(), lambdas)
                ).at(term.start, lambdas.last().value.next))
        }
    }
}

fun Parser.parsePostfixLambdas(): List<Arg<PExpr>> {
    val list = mutableListOf(
        Arg(
            null,
            parseLambdaOr { return emptyList() })
    )
    return recBuildList(list) {
        val ident = current
        if (ident is Token.Ident && next == Token.Colon) {
            add(Arg(ident.name.end(), advance().parseLambdaOr {
                err("Missing lambda")
                Expr.Error.noPos()
            }))
        } else return list
    }
}

private inline fun Parser.parseLambdaOr(or: () -> PExpr) = when (val curr = current) {
    is Token.Ident -> if (rawLookup(1) == Token.At) {
        val start = pos
        advance()

        val label = curr.name.end(start)

        val braces = current
        if (braces is Token.Braces) {
            parseLambda(label, braces.tokens)
        } else {
            err("Missing lambda")
            Expr.Error.at(start, pos)
        }
    } else or()
    is Token.Braces -> parseLambda(null, curr.tokens)
    else -> or()
}

fun Parser.parseSubExpr() = parseTerm()?.let { parsePostfix(it) }
fun Parser.requireSubExpr() = parseSubExpr() ?: expectedExpression()

fun Parser.parseExpr(): PExpr? = parseSubExpr()?.let { first ->
    parseBinOps(first, parseOp() ?: return first, -1).first
}

fun Parser.parseBinOpsOnLine(): PExpr? = if (lastNewline) null else parseExpr()

fun Parser.parseOp(): PBinOp? = when (val token = current) {
    Token.Plus -> if (currentInfix()) Plus.end() else null
    Token.Minus -> if (currentInfix()) Minus.end() else null
    Token.Times -> Times.end()
    Token.Div -> Div.end()
    Token.Rem -> Rem.end()
    Token.RangeTo -> RangeTo.end()
    Token.Eq -> Eq.end()
    Token.RefEq -> RefEq.end()
    Token.NotEq -> NotEq.end()
    Token.NotRefEq -> NotRefEq.end()
    Token.As -> when (rawLookup(1)) {
        Token.QuestionMark -> {
            val asStart = pos
            advance()
            NullableAs.end(asStart)
        }
        Token.ExclamationMark -> {
            val asStart = pos
            advance()
            UnsafeAs.end(asStart)
        }
        else -> As.end()
    }
    Token.Is -> Is.end()
    Token.Elvis -> Elvis.end(pos - 1)
    Token.Greater -> Greater.end()
    Token.Less -> Less.end()
    Token.GreaterOrEq -> GreaterOrEq.end()
    Token.LessOrEq -> LessOrEq.end()
    Token.LogicalAnd -> And.end()
    Token.LogicalOr -> Or.end()
    Token.Assign -> Assign.end()
    Token.PlusAssign -> PlusAssign.end()
    Token.MinusAssign -> MinusAssign.end()
    Token.TimesAssign -> TimesAssign.end()
    Token.DivAssign -> DivAssign.end()
    Token.RemAssign -> RemAssign.end()
    Token.ExclamationMark -> if (lastNewline) null else when (val next = rawLookup(1)) {
        is Token.Is -> {
            val start = pos
            advance()
            IsNot.end(start)
        }
        is Token.Ident -> {
            val start = pos
            advance()
            Infix(next.name, true).end(start)
        }
        else -> null
    }
    is Token.Ident -> if (lastNewline) null else Infix(token.name, false).end()
    else -> null
}

tailrec fun Parser.parseBinOps(left: PExpr, op: PBinOp, minPrecedence: Int): Pair<PExpr, PBinOp?> {
    return if (op.value.precedence > minPrecedence) {
        val (nextLeft, nextOp) = op.value.handle(op.start, op.next, left, this)
        parseBinOps(nextLeft, nextOp ?: return nextLeft to null, minPrecedence)
    } else left to op
}

fun Parser.requireExpr(): PExpr =
    parseExpr() ?: expectedExpression()

fun Parser.parseEqExpr(): PExpr? = if (current == Token.Assign) {
    advance().requireExpr()
} else {
    null
}

fun Parser.requireEqExpr(): PExpr = if (current == Token.Assign) advance().requireExpr() else expectedExpression()

private fun Parser.parseParenthesized(tokens: List<PToken>): PExpr =
    parenthesizedOf(tokens).parseCommaSeparated(Parser::requireExpr) { exprs, trailing ->
        if (exprs.size == 1 && !trailing) Expr.Parenthesized(exprs[0])
        else Expr.Tuple(exprs)
    }.end()

private fun Parser.parseWhenBranch(): WhenBranch {
    val pattern = parsePattern()
    val guard = if (current == Token.Pipe) BranchGuard(advance().requireExpr()) else null
    val res = when (current) {
        Token.When -> {
            val expr = advance().inParensOr(Parser::requireExpr) { null }
            val branches = inBracesOr(Parser::parseWhenBranches) {
                err("Missing when body")
                emptyList()
            }
            BranchRes.Branching(expr, branches)
        }
        Token.Arrow -> BranchRes.Single(advance().requireExpr())
        else -> BranchRes.Single(err("Missing arrow").requireExpr())
    }
    return WhenBranch(pattern, guard, res)
}

fun Parser.parseWhenBranches(): List<WhenBranch> = recBuildList {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            add(parseWhenBranch())
            val sep = current
            when {
                sep == Token.Semicolon -> advance()
                sep == Token.End -> return this
                lastNewline -> {

                }
                else -> err("Unclosed when")
            }
        }
    }
}

private fun Parser.parseWhen(): PExpr = withPos { start ->
    val expr = advance().inParensOr(Parser::requireExpr) { null }
    val afterBranches = nextPos
    val branches = inBracesOr(Parser::parseWhenBranches) {
        err("Missing when body")
        emptyList()
    }

    Expr.When(expr, branches).at(start, afterBranches)
}

private fun Parser.parseListBody(list: MutableList<PExpr>): Expr = recBuildListN(list) {
    if (current == Token.End) {
        return Expr.Lis(this)
    } else {
        add(requireExpr())
        when (current) {
            Token.Comma -> advance()
            Token.End -> return Expr.Lis(this)
            else -> err("Missing comma")
        }
    }
}

inline fun <T> Parser.missingComma(fn: () -> T): T {
    err("Missing comma", pos, pos)
    return fn()
}

private fun Parser.parseMapBody(list: MutableList<Pair<PExpr, PExpr>>): Expr = recBuildListN(list) {
    if (current == Token.End) {
        return Expr.Map(this)
    } else {
        val first = requireExpr()
        if (current == Token.Colon) advance() else err("Missing colon")
        val second = requireExpr()
        add(first to second)

        when (current) {
            Token.Comma -> advance()
            Token.End -> return Expr.Map(this)
            else -> err("Missing comma")
        }
    }
}

private fun Parser.parseListOrMap(tokens: List<PToken>): PExpr =
    with(parenthesizedOf(tokens)) {
        when (current) {
            Token.End -> Expr.Lis(emptyList())
            Token.Colon -> {
                if (advance().current != Token.End) err("Missing key")
                Expr.Map(emptyList())
            }
            else -> {
                val first = requireExpr()
                when (current) {
                    Token.Comma -> advance().parseListBody(mutableListOf(first))
                    Token.Colon -> {
                        val second = advance().requireExpr()
                        when (current) {
                            Token.Comma -> advance().parseMapBody(mutableListOf(first to second))
                            Token.End -> Expr.Map(listOf(first to second))
                            else -> missingComma {
                                advance().parseMapBody(mutableListOf(first to second))
                            }
                        }
                    }
                    Token.End -> Expr.Lis(listOf(first))
                    else -> missingComma {
                        advance().parseListBody(mutableListOf(first))
                    }
                }
            }
        }
    }.end()

private fun Parser.parseGet(on: PExpr, tokens: List<PToken>): PExpr {
    val arg = parenthesizedOf(tokens).requireExpr()

    return Expr.Get(on, arg).end(on.start)
}

private fun Parser.parseOptionallyTypedParams() = recBuildList<Pair<PDecPattern, PType?>> {
    if (current == Token.End) return this
    add(requireDecPattern() to parseTypeAnn())
    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

private fun Parser.parseLambdaHeader(): LambdaHeader? {
    val startIndex = index
    val tempErrors = mutableListOf<PalmError>()
    val contextParams = inBracketsOrEmpty(tempErrors, Parser::parseOptionallyTypedParams)
    val mainParams = inParensOr(tempErrors, Parser::parseOptionallyTypedParams) {
        index = startIndex
        return null
    }
    val returnType = parseTypeAnn()

    if (current == Token.Arrow) {
        errors.addAll(tempErrors)
        advance()
    } else {
        index = startIndex
        return null
    }

    return LambdaHeader(contextParams, mainParams, returnType)
}

private fun Parser.parseLambda(label: PString?, tokens: List<PToken>): PExpr = with(scopedOf(tokens)) {
    val params = parseLambdaHeader()
    val body = parseStatements()
    Expr.Lambda(label, params, body)
}.end()

fun Parser.parseCallArgs(): List<Arg<PExpr>> =
    recBuildList {
        val exprStart = current
        if (exprStart == Token.End) return this

        add(
            if (exprStart is Token.Ident && next == Token.Colon) {
                val ident = exprStart.name.end()
                val expr = advance().requireExpr()
                Arg(ident, expr)
            } else {
                Arg(null, requireExpr())
            }
        )

        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Missing comma")
        }
    }

private fun Parser.parseCall(on: PExpr, tokens: List<PToken>): PExpr {
    val afterParens = nextPos
    val list = parenthesizedOf(tokens).parseCallArgs()
    val after = advance().parsePostfixLambdas()

    return Expr.Call(on,
        CallArgs(list, after)
    ).at(on.start, after.lastOrNull()?.value?.next ?: afterParens)
}

private fun Parser.parseContextCall(on: PExpr, tokens: List<PToken>): PExpr {
    val list = parenthesizedOf(tokens).parseCallArgs().also { advance() }
    return Expr.ContextCall(on, list).end(on.start)
}

private fun Parser.parseTurbofish(on: PExpr): PExpr {
    val args = advance().parseTypeArgs()
    return Expr.Turbofish(on, args).end(on.start)
}