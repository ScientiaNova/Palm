package com.scientianova.palm.parser

import com.scientianova.palm.lexer.TokenIterator
import com.scientianova.palm.lexer.toList

fun testLex(code: String) = TokenIterator(code).toList()

fun testLexString(code: String) = testLex(code).joinToString("\n") { it.value.toCodeString() }

fun testExprLex() = testLex(
    """
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