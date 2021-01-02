package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.types.parseClassSuperTypes
import com.scientianova.palm.parser.parsing.types.parseObjectBody
import com.scientianova.palm.util.*

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
    is Token.Parens -> parseTuple(token.tokens)
    Token.NullLit -> Expr.Null.end()
    Token.Super -> Expr.Super.end()
    Token.Return -> parseReturn()
    Token.Break -> parseBreak()
    Token.If -> parseIf()
    Token.When -> parseWhen()
    Token.Throw -> withPos { advance().requireBinOps().let { expr -> Expr.Throw(expr).at(it, expr.next) } }
    Token.Do -> withPos { advance().requireScope().let { scope -> Expr.Do(scope, parseCatches()).at(it, scope.next) } }
    Token.Object -> parseObject()
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
        when (val afterQ = advance().current) {
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
            parsePostfix(Expr.Call(term, CallArgs(emptyList(), lambdas)).at(term.start, lambdas.last().value.next))
        }
    }
}

fun Parser.parsePostfixLambdas(): List<Arg<PExpr>> {
    val list = mutableListOf(Arg(null, parseLambdaOr { return emptyList() }))
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

fun Parser.parseBinOps(): PExpr? = parseSubExpr()?.let { first ->
    parseBinOps(first, parseOp() ?: return first, -1).first
}

fun Parser.parseBinOpsOnLine(): PExpr? = if (lastNewline) null else parseBinOps()

fun Parser.parseOp(): PBinOp? {
    val token = current
    return when {
        token == Token.Plus && currentInfix() -> Plus.end()
        token == Token.Minus && currentInfix() -> Minus.end()
        token == Token.Times -> Times.end()
        token == Token.Div -> Div.end()
        token == Token.Rem -> Rem.end()
        token == Token.RangeTo -> RangeTo.end()
        token == Token.Eq -> Eq.end()
        token == Token.RefEq -> RefEq.end()
        token == Token.NotEq -> Eq.end().let { Not(it).at(it.start, it.next) }
        token == Token.NotRefEq -> RefEq.end().let { Not(it).at(it.start, it.next) }
        token == Token.As -> when (rawLookup(1)) {
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
        token == Token.Is && !lastNewline -> Is.end()
        token == Token.In && !lastNewline -> In.end()
        token == Token.QuestionMark && rawLookup(1) == Token.Colon -> {
            advance()
            Elvis.end(pos - 1)
        }
        token == Token.Greater -> Greater.end()
        token == Token.Less -> Less.end()
        token == Token.GreaterOrEq -> GreaterOrEq.end()
        token == Token.LessOrEq -> LessOrEq.end()
        token == Token.And -> And.end()
        token == Token.Or -> Or.end()
        token == Token.Assign -> Assign.end()
        token == Token.PlusAssign -> PlusAssign.end()
        token == Token.MinusAssign -> MinusAssign.end()
        token == Token.TimesAssign -> TimesAssign.end()
        token == Token.DivAssign -> DivAssign.end()
        token == Token.RemAssign -> RemAssign.end()
        token == Token.ExclamationMark && !lastNewline -> when (val next = rawLookup(1)) {
            is Token.Is -> {
                val start = pos
                advance()
                Is.end().let { Not(it).at(start, it.next) }
            }
            is Token.In -> {
                val start = pos
                advance()
                In.end().let { Not(it).at(start, it.next) }
            }
            is Token.Ident -> {
                val start = pos
                advance()
                Infix(next.name).end().let { Not(it).at(start, it.next) }
            }
            else -> null
        }
        token is Token.Ident && !lastNewline -> Infix(token.name).end()
        else -> null
    }
}

tailrec fun Parser.parseBinOps(left: PExpr, op: PBinOp, minPrecedence: Int): Pair<PExpr, PBinOp?> {
    return if (op.value.precedence > minPrecedence) {
        val (nextLeft, nextOp) = op.value.handle(op.start, op.next, left, this)
        parseBinOps(nextLeft, nextOp ?: return nextLeft to null, minPrecedence)
    } else left to op
}

fun Parser.requireBinOps(): PExpr =
    parseBinOps() ?: expectedExpression()

fun Parser.requireScopeOrBinOps(): PExpr = parseScope()?.map(Expr::Scope) ?: requireBinOps()

fun Parser.parseEqExpr(): PExpr? = if (current == Token.Assign) {
    advance().requireBinOps()
} else {
    null
}

private fun Parser.parseTupleBody(): List<PExpr> = recBuildList {
    if (current == Token.End)
        return this

    add(requireBinOps())
    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

private fun Parser.parseTuple(tokens: List<PToken>): PExpr {
    val list = parenthesizedOf(tokens).parseTupleBody()

    return if (list.size == 1) {
        advance()
        list[0]
    } else {
        Expr.Tuple(list).end()
    }
}

private fun Parser.parseWhenBranch(): WhenBranch {
    val pattern = parsePattern()
    val guard = if (current == Token.If) BranchGuard(advance().requireBinOps()) else null
    val res = when (current) {
        Token.When -> {
            val expr = advance().inParensOr(Parser::requireBinOps) { null }
            val branches = inBracesOr(Parser::parseWhenBranches) {
                err("Missing when body")
                emptyList()
            }
            BranchRes.Branching(expr, branches)
        }
        Token.Arrow -> BranchRes.Single(advance().requireScopeOrBinOps())
        else -> BranchRes.Single(err("Missing arrow").requireScopeOrBinOps())
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
    val expr = advance().inParensOr(Parser::requireBinOps) { null }
    val afterBranches = nextPos
    val branches = inBracesOr(Parser::parseWhenBranches) {
        err("Missing when body")
        emptyList()
    }

    Expr.When(expr, branches).at(start, afterBranches)
}


private fun Parser.parseIf(): PExpr = withPos { start ->
    val condition = advance().inParensOr(Parser::requireBinOps) {
        err("Missing condition")
        Expr.Error.noPos()
    }
    val ifScope = requireScopeOrBinOps()
    val elseScope = if (current == Token.Else) {
        advance().requireScopeOrBinOps()
    } else {
        null
    }

    return Expr.If(condition, ifScope, elseScope).at(start, elseScope?.next ?: ifScope.next)
}

private fun Parser.parseListBody(list: MutableList<PExpr>): List<PExpr> = recBuildList(list) {
    if (current == Token.End) {
        return this
    } else {
        add(requireBinOps())
        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Missing comma")
        }
    }
}

inline fun <T> Parser.missingComma(fn: () -> T): T {
    err("Missing comma", pos, pos)
    return fn()
}

private fun Parser.parseMapBody(list: MutableList<Pair<PExpr, PExpr>>) = recBuildList(list) {
    if (current == Token.End) {
        return this
    } else {
        val first = requireBinOps()
        if (current == Token.Colon) advance() else err("Missing colon")
        val second = requireBinOps()
        add(first to second)

        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
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
                val first = requireBinOps()
                when (current) {
                    Token.Comma -> Expr.Lis(advance().parseListBody(mutableListOf(first)))
                    Token.Colon -> {
                        val second = advance().requireBinOps()
                        when (current) {
                            Token.Comma -> Expr.Map(advance().parseMapBody(mutableListOf(first to second)))
                            Token.End -> Expr.Map(listOf(first to second))
                            else -> missingComma {
                                Expr.Map(advance().parseMapBody(mutableListOf(first to second)))
                            }
                        }
                    }
                    Token.End -> Expr.Lis(listOf(first))
                    else -> missingComma {
                        Expr.Lis(advance().parseListBody(mutableListOf(first)))
                    }
                }
            }
        }
    }.end()

private fun Parser.parseGet(on: PExpr, tokens: List<PToken>): PExpr {
    val list = parenthesizedOf(tokens).parseListBody(mutableListOf())

    return if (list.isEmpty()) {
        err("Empty get").advance()
        on
    } else {
        Expr.Get(on, list).end(on.start)
    }
}

private fun Parser.parseOptionallyTypedParams() = recBuildList<Pair<PDecPattern, PType>> {
    if (current == Token.End) return this
    add(requireDecPattern() to (parseTypeAnn() ?: Type.Infer.noPos()))
    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

private fun Parser.parseLambdaParams(): LambdaParams? {
    val startIndex = index
    val contextParams = inBracketsOrEmpty(Parser::parseOptionallyTypedParams)
    val mainParams = inParensOr(Parser::parseOptionallyTypedParams) {
        index = startIndex
        return null
    }

    if (current == Token.Arrow) advance() else {
        index = startIndex
        return null
    }

    return LambdaParams(contextParams, mainParams)
}

private fun Parser.parseLambda(label: PString?, tokens: List<PToken>): PExpr = with(scopedOf(tokens)) {
    val params = parseLambdaParams()
    val body = parseStatements()
    Expr.Lambda(label, params, body)
}.end()

fun Parser.parseCallArgs(): List<Arg<PExpr>> =
    recBuildList {
        val exprStart = current
        if (exprStart == Token.End) return this

        add(if (exprStart is Token.Ident && next == Token.Colon) {
            val ident = exprStart.name.end()
            val expr = advance().requireBinOps()
            Arg(ident, expr)
        } else {
            Arg(null, requireBinOps())
        })

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

    return Expr.Call(on, CallArgs(list, after)).at(on.start, after.lastOrNull()?.value?.next ?: afterParens)
}

private fun Parser.parseContextCall(on: PExpr, tokens: List<PToken>): PExpr {
    val list = parenthesizedOf(tokens).parseCallArgs().also { advance() }
    return Expr.ContextCall(on, list).end(on.start)
}

private fun Parser.parseTurbofish(on: PExpr): PExpr {
    val args = advance().parseTypeArgs()
    return Expr.Turbofish(on, args).end(on.start)
}

private fun Parser.parseObject(): PExpr = withPos { start ->
    val afterObj = nextPos
    val superTypes = advance().parseClassSuperTypes()
    val nextPos: StringPos

    val body = current.let { braces ->
        if (braces is Token.Braces) {
            scopedOf(braces.tokens).parseObjectBody().also {
                nextPos = this.nextPos
                advance()
            }
        } else {
            nextPos = superTypes.lastOrNull()?.next ?: afterObj
            err("Missing object body")
            emptyList()
        }
    }

    return Expr.Object(superTypes, body).at(start, nextPos)
}