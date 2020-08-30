package com.scientianova.palm.parser

import com.scientianova.palm.errors.PError
import com.scientianova.palm.errors.messageFor
import com.scientianova.palm.util.Either
import com.scientianova.palm.util.Left
import com.scientianova.palm.util.Right

fun testParse(
    parser: Parser<Either<PError, String>, String>,
    code: String
): String = when (val either = parse(ParseState(code, 0), parser)) {
    is Right -> either.right
    is Left -> either.left.messageFor(code, "REPL")
}

fun testExpr() = testParse(
    scopedExpr<Either<PError, String>>().map { it.toCodeString(0) }, """
    if state.char?.isSymbolPart() == true {
        val op = handleSymbol(state)
        if op.value.isPostfixOp(afterOp) {
            list.map(op.area) { expr ->
                PostfixOpExpr(op, expr) at expr.area.first...op.area.last succTo afterOp
            }.flatMap { newList, next ->
                return handleInlinedBinOps(next, start, excludeCurly, newList)
            }
        } else { 
            handleSubexpr(afterOp.actual, false).flatMap { sub, next ->
              return handleInlinedBinOps(next, start, excludeCurly, list.appendSymbol(op, sub))
            } 
        }
    } else {
        val actual = state.actual
        when actual.char {
            null -> finishBinOps(start, list, state)
            identStartChars -> {
                val infix = handleIdent(actual)
                when infix.value {
                    keywords -> finishBinOps(start, list, state)
                    "is" -> handleType(afterInfix.actual, false).flatMap { type, next ->
                        return handleInlinedBinOps(next, start, excludeCurly, list.appendIs(type))
                    }
                    "as" -> {
                        val handling = when afterInfix.char {
                            '!' -> AsHandling.Unsafe to afterInfix.nextActual
                            '?' -> AsHandling.Nullable to afterInfix.nextActual
                            _ -> AsHandling.Safe to afterInfix.actual
                        }
                        handleType(typeStart, false).flatMap { type, next ->
                            return handleInlinedBinOps(next, start, excludeCurly, list.appendAs(type, handling))
                        }
                    }
                    _ -> handleSubexpr(afterInfix.actual, false).flatMap { part, next ->
                        return handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                    }
                }
            }
            '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
                handleSubexpr(afterInfix.nextActual, false).flatMap { part, next ->
                    return handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                }
            }
            symbolChars -> {
                val op = handleSymbol(state)
                val symbol = op.value
                if !(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false {
                    invalidPrefixOperatorError errAt afterOp.pos
                } else {
                    when symbol {
                        "->" -> finishBinOps(start, list, state)
                        _ -> handleSubexpr(afterOp.actual, false).flatMap { part, next ->
                            return handleInlinedBinOps(next.actual, start, excludeCurly, list.appendSymbol(op, part))
                        }
                    }
                }
            }
            '(' -> list.map(state.pos) { expr -> handleCall(state, expr, excludeCurly) }.flatMap { newList, next ->
                return handleInlinedBinOps(next, start, excludeCurly, newList)
            }
            '{' -> if excludeCurly {
                finishBinOps(start, list, state)
            } else { 
                list.map(state.pos) { expr ->
                    handleLambda(state.nextActual, state.pos).map { lambda ->
                        CallExpr(expr, CallParams(listOf(lambda))) at expr.area.first..lambda.area.first
                    }
                }
            }.flatMap { nextList, next -> return handleInlinedBinOps(next, start, excludeCurly, nextList) }
            _ -> finishBinOps(start, list, state)
        }
    }
""".trimIndent()
)