package com.scientianova.palm.lexer

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

internal tailrec fun lexSingleLineString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    null, '\n' -> missingDoubleQuote errAt pos
    '"' -> Token.Str(parts + StringPart.Regular(builder.toString())) succTo pos + 1
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' -> when (val res = interLex(code, interPos + 1, listOf(Token.LBrace at interPos), 1, false)) {
                is LexResult.Success -> {
                    val nextPos = res.next
                    lexSingleLineString(
                        code, nextPos, listOf(
                            *parts.toTypedArray(), StringPart.Regular(builder.toString()),
                            StringPart.List(res.value)
                        ), StringBuilder()
                    )
                }
                is LexResult.Error -> res
            }
            '`' -> when (val res = lexTickedIdent(code, interPos + 1, StringBuilder())) {
                is LexResult.Success -> {
                    val nextPos = res.next
                    lexSingleLineString(
                        code, nextPos, listOf(
                            *parts.toTypedArray(), StringPart.Regular(builder.toString()),
                            StringPart.SingleToken(res.value.at(interPos))
                        ), StringBuilder()
                    )
                }
                is LexResult.Error -> res
            }
            in identStartChars -> {
                val (token, nextPos) = lexNormalIdent(code, interPos + 1, StringBuilder().append(interChar))
                lexSingleLineString(
                    code, nextPos, listOf(
                        *parts.toTypedArray(), StringPart.Regular(builder.toString()),
                        StringPart.SingleToken(token.at(interPos))
                    ), StringBuilder()
                )
            }
            else -> lexSingleLineString(code, pos + 1, parts, builder.append('$'))
        }
    }
    '\\' -> when (val res = handleEscaped(code, pos + 1)) {
        is LexResult.Success ->
            lexSingleLineString(code, res.next, parts, builder.append(res.value))
        is LexResult.Error -> res
    }
    else -> lexSingleLineString(code, pos + 1, parts, builder.append(char))
}

internal tailrec fun lexMultiLineString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    null -> unclosedMultilineString errAt pos
    '"' -> if (code.startsWith("\"\"", pos + 1)) {
        Token.Str(parts + StringPart.Regular(builder.toString())) succTo pos + 1
    } else lexMultiLineString(code, pos + 1, parts, builder.append('"'))
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' -> when (val res = interLex(code, interPos + 1, listOf(Token.LBrace at interPos), 1, false)) {
                is LexResult.Success -> {
                    val nextPos = res.next
                    lexMultiLineString(
                        code, nextPos, listOf(
                            *parts.toTypedArray(), StringPart.Regular(builder.toString()),
                            StringPart.List(res.value)
                        ), StringBuilder()
                    )
                }
                is LexResult.Error -> res
            }
            in identStartChars -> {
                val (token, nextPos) = lexNormalIdent(code, interPos + 1, StringBuilder().append(interChar))
                lexMultiLineString(
                    code, nextPos, listOf(
                        *parts.toTypedArray(), StringPart.Regular(builder.toString()),
                        StringPart.SingleToken(token.at(interPos))
                    ), StringBuilder()
                )
            }
            else -> lexMultiLineString(code, pos + 1, parts, builder.append('$'))
        }
    }
    else -> lexMultiLineString(code, pos + 1, parts, builder.append(char))
}

private tailrec fun interLex(
    code: String,
    pos: StringPos,
    list: TokenList,
    scopesIn: Int,
    isPostfix: Boolean,
): LexResult<TokenList> = when (val char = code.getOrNull(pos)) {
    null -> unclosedInterpolation errAt pos
    '\n' -> interLex(code, pos + 1, list + Token.EOL.at(pos), scopesIn, false)
    '\t', ' ', '\r' -> interLex(code, pos + 1, list, scopesIn, false)
    '(' -> interLex(code, pos + 1, list + Token.LParen.at(pos), scopesIn, false)
    '[' -> interLex(code, pos + 1, list + Token.LBracket.at(pos), scopesIn, false)
    '{' -> interLex(code, pos + 1, list + Token.LBrace.at(pos), scopesIn + 1, false)
    ')' -> interLex(code, pos + 1, list + Token.RParen.at(pos), scopesIn, true)
    ']' -> interLex(code, pos + 1, list + Token.RBracket.at(pos), scopesIn, true)
    '}' -> if (scopesIn == 1) {
        list + Token.RBrace.at(pos) succTo pos + 1
    } else {
        interLex(code, pos + 1, list + Token.RBrace.at(pos), scopesIn, true)
    }
    ',' -> interLex(code, pos + 1, list + Token.Comma.at(pos), scopesIn, false)
    ';' -> interLex(code, pos + 1, list + Token.Semicolon.at(pos), scopesIn, false)
    '@' -> interLex(code, pos + 1, list + Token.At.at(pos), scopesIn, false)
    ':' -> if (code.getOrNull(pos + 1) == ':') {
        interLex(code, pos + 2, list + Token.DoubleColon.at(pos), scopesIn, false)
    } else {
        interLex(code, pos + 1, list + Token.Colon.at(pos), scopesIn, false)
    }
    '.' -> if (code.getOrNull(pos + 1) == '.') {
        val isPrefix = code.getOrNull(pos + 2).isAfterPrefix()
        when {
            isPostfix == isPrefix -> interLex(code, pos + 2, list + Token.RangeTo.at(pos), scopesIn, false)
            isPrefix -> interLex(code, pos + 2, list + Token.RangeFrom.at(pos), scopesIn, false)
            else -> interLex(code, pos + 2, list + Token.RangeUntil.at(pos), scopesIn, false)
        }
    } else {
        interLex(code, pos + 1, list + Token.Dot.at(pos), scopesIn, false)
    }
    '0' -> {
        val res = when (code.getOrNull(pos + 1)) {
            'b', 'B' -> lexBinaryNumber(code, pos + 2, StringBuilder())
            'x', 'X' -> lexHexNumber(code, pos + 2, StringBuilder())
            else -> lexNumber(code, pos + 1, StringBuilder("0"))
        }
        when (res) {
            is LexResult.Success -> {
                val nextPos = res.next
                interLex(code, nextPos, list + res.value.at(pos), scopesIn, true)
            }
            is LexResult.Error -> (res.error errAt res.area)
        }
    }
    in '1'..'9' -> when (val res = lexNumber(code, pos + 1, StringBuilder().append(char))) {
        is LexResult.Success -> {
            val nextPos = res.next
            interLex(code, nextPos, list + res.value.at(pos), scopesIn, true)
        }
        is LexResult.Error -> (res.error errAt res.area)
    }
    '\'' -> when (val res = lexChar(code, pos + 1)) {
        is LexResult.Success -> {
            val nextPos = res.next
            interLex(code, nextPos, list + res.value.at(pos), scopesIn, true)
        }
        is LexResult.Error -> (res.error errAt res.area)
    }
    '\"' -> if (code.getOrNull(pos + 1) == '\"') {
        if (code.getOrNull(pos + 2) == '\"') {
            when (val res = lexMultiLineString(code, pos + 3, emptyList(), StringBuilder())) {
                is LexResult.Success -> {
                    val nextPos = res.next
                    interLex(code, nextPos, list + res.value.at(pos), scopesIn, true)
                }
                is LexResult.Error -> res.error errAt res.area
            }
        } else {
            interLex(code, pos + 2, list + emptyStr.at(pos), scopesIn, true)
        }
    } else {
        when (val res = lexSingleLineString(code, pos + 1, emptyList(), StringBuilder())) {
            is LexResult.Success -> {
                val nextPos = res.next
                interLex(code, nextPos, list + res.value.at(pos), scopesIn, true)
            }
            is LexResult.Error -> (res.error errAt res.area)
        }
    }
    in identStartChars -> {
        val (token, nextPos) = lexNormalIdent(code, pos + 1, StringBuilder().append(char))
        interLex(code, nextPos, list + token.at(pos), scopesIn, true)
    }
    '`' -> when (val res = lexTickedIdent(code, pos + 1, StringBuilder())) {
        is LexResult.Success -> {
            val nextPos = res.next
            interLex(code, nextPos, list + res.value.at(pos), scopesIn, true)
        }
        is LexResult.Error -> res.error errAt res.area
    }
    '+' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '+' -> unsupportedOperator("++", "+= 1").errAt(pos)
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.PlusAssign.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("+=").errAt(pos)
            }
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                when {
                    isPostfix == isPrefix -> interLex(code, pos + 1, list + Token.Plus.at(pos), scopesIn, false)
                    isPrefix -> interLex(code, pos + 1, list + Token.UnaryPlus.at(pos), scopesIn, false)
                    else -> unknownPostfixOp("+").errAt(pos)
                }
            }
        }
    }
    '-' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '>' -> interLex(code, pos + 2, list + Token.Arrow.at(pos), scopesIn, false)
            '-' -> unsupportedOperator("--", "-= 1").errAt(pos)
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.MinusAssign.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("-=").errAt(pos)
            }
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                when {
                    isPostfix == isPrefix -> interLex(code, pos + 1, list + Token.Minus.at(pos), scopesIn, false)
                    isPrefix -> interLex(code, pos + 1, list + Token.UnaryMinus.at(pos), scopesIn, false)
                    else -> (unknownPostfixOp("-").errAt(pos))
                }
            }
        }
    }
    '*' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '*' -> unsupportedOperator("*", "pow").errAt(pos)
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.TimesAssign.at(pos), scopesIn, false)
            } else {
                (invalidInfixOp("*=").errAt(pos))
            }
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                when {
                    isPostfix == isPrefix -> interLex(code, pos + 1, list + Token.Times.at(pos), scopesIn, false)
                    isPrefix -> interLex(code, pos + 1, list + Token.Spread.at(pos), scopesIn, false)
                    else -> (unknownPostfixOp("*").errAt(pos))
                }
            }
        }
    }
    '/' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '/' -> interLex(code, lexSingleLineComment(code, pos + 1), list, scopesIn, false)
            '*' -> interLex(code, lexMultiLineComment(code, pos + 1), list, scopesIn, false)
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.DivAssign.at(pos), scopesIn, false)
            } else {
                (invalidInfixOp("/=").errAt(pos))
            }
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                interLex(code, pos + 1, list + Token.Div.at(pos), scopesIn, false)
            } else {
                (invalidInfixOp("/") errAt pos)
            }
        }
    }
    '%' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.RemAssign.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("%=").errAt(pos)
            }
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                interLex(code, pos + 1, list + Token.Rem.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("%") errAt pos
            }
        }
    }
    '?' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '.' -> interLex(code, pos + 2, list + Token.SafeAccess.at(pos), scopesIn, false)
            ':' -> if (isOpInfix(code, isPostfix, pos + 2)) {
                interLex(code, pos + 2, list + Token.Elvis.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("?:").errAt(pos)
            }
            else -> interLex(code, nextPos, list + Token.QuestionMark.at(pos), scopesIn, false)
        }
    }
    '&' -> {
        val nextPos = pos + 1
        if (code.getOrNull(nextPos) == '&') {
            if (isOpInfix(code, isPostfix, pos + 2)) {
                interLex(code, pos + 2, list + Token.And.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("&&").errAt(pos)
            }
        } else (unsupportedOperator("&", "and") errAt pos)
    }
    '|' -> {
        val nextPos = pos + 1
        if (code.getOrNull(nextPos) == '|') {
            if (isOpInfix(code, isPostfix, pos + 2)) {
                interLex(code, pos + 2, list + Token.Or.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("||").errAt(pos)
            }
        } else (unsupportedOperator("|", "or") errAt pos)
    }
    '<' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.LessOrEq.at(pos), scopesIn, false)
            } else {
                invalidInfixOp("<=").errAt(pos)
            }
            '<' -> (unsupportedOperator("<<", "shl").errAt(pos))
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                interLex(code, nextPos, list + Token.Less.at(pos), scopesIn, false)
            } else {
                invalidPrefixOp("<") errAt pos
            }
        }
    }
    '>' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.GreaterOrEq.at(pos), scopesIn, false)
            } else {
                invalidInfixOp(">=").errAt(pos)
            }
            '>' -> (unsupportedOperator(">>", "shr").errAt(pos))
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                interLex(code, nextPos, list + Token.Greater.at(pos), scopesIn, false)
            } else {
                invalidPrefixOp(">") errAt pos
            }
        }
    }
    '!' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> when (code.getOrNull(nextPos + 1)) {
                '=' -> if (isOpInfix(code, isPostfix, nextPos + 2)) {
                    interLex(code, nextPos + 2, list + Token.NotRefEq.at(pos + 1), scopesIn, false)
                } else {
                    invalidInfixOp("!==").errAt(pos + 1)
                }
                else -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                    interLex(code, nextPos + 1, list + Token.NotEq.at(pos), scopesIn, false)
                } else {
                    invalidInfixOp("!=").errAt(pos)
                }
            }
            '!' -> if (isOpPostfix(code, isPostfix, nextPos + 1)) {
                interLex(code, nextPos + 1, list + Token.DoubleExclamation.at(pos), scopesIn, false)
            } else {
                invalidPostfixOp("!!").errAt(pos)
            }
            else -> if (isOpPrefix(code, isPostfix, nextPos)) {
                interLex(code, nextPos, list + Token.Not.at(pos), scopesIn, false)
            } else {
                invalidPrefixOp("!") errAt pos
            }
        }
    }
    '=' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> when (code.getOrNull(nextPos + 1)) {
                '=' -> if (isOpInfix(code, isPostfix, nextPos + 2)) {
                    interLex(code, nextPos + 2, list + Token.RefEq.at(pos + 1), scopesIn, false)
                } else {
                    invalidInfixOp("===").errAt(pos + 1)
                }
                else -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                    interLex(code, nextPos + 1, list + Token.Eq.at(pos), scopesIn, false)
                } else {
                    invalidInfixOp("==").errAt(pos)
                }
            }
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                interLex(code, nextPos, list + Token.Assign.at(pos), scopesIn, false)
            } else {
                (invalidInfixOp("=") errAt pos)
            }
        }
    }
    else -> errorSymbol(char) errAt pos
}