package com.scientianova.palm.lexer

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.*

tailrec fun lex(
    code: String,
    pos: StringPos,
    list: TokenList,
    isPostfix: Boolean
): Either<PError, TokenList> = when (val char = code.getOrNull(pos)) {
    null -> Right(list + Token.EOF.at(pos))
    '\n' -> lex(code, pos + 1, list + Token.EOL.at(pos), false)
    '\t', ' ', '\r' -> lex(code, pos + 1, list, false)
    '(' -> lex(code, pos + 1, list + Token.LParen.at(pos), false)
    '[' -> lex(code, pos + 1, list + Token.LBracket.at(pos), false)
    '{' -> lex(code, pos + 1, list + Token.LBrace.at(pos), false)
    ')' -> lex(code, pos + 1, list + Token.RParen.at(pos), true)
    ']' -> lex(code, pos + 1, list + Token.RBracket.at(pos), true)
    '}' -> lex(code, pos + 1, list + Token.RBrace.at(pos), true)
    ',' -> lex(code, pos + 1, list + Token.Comma.at(pos), false)
    ';' -> lex(code, pos + 1, list + Token.Semicolon.at(pos), false)
    '@' -> lex(code, pos + 1, list + Token.At.at(pos), false)
    ':' -> if (code.getOrNull(pos + 1) == ':') {
        lex(code, pos + 2, list + Token.DoubleColon.at(pos), false)
    } else {
        lex(code, pos + 1, list + Token.Colon.at(pos), false)
    }
    '.' -> if (code.getOrNull(pos + 1) == '.') {
        val isPrefix = code.getOrNull(pos + 2).isAfterPrefix()
        when {
            isPostfix == isPrefix -> lex(code, pos + 2, list + Token.RangeTo.at(pos), false)
            isPrefix -> lex(code, pos + 2, list + Token.RangeFrom.at(pos), false)
            else -> lex(code, pos + 2, list + Token.RangeUntil.at(pos), false)
        }
    } else {
        lex(code, pos + 1, list + Token.Dot.at(pos), false)
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
                lex(code, nextPos, list + res.value.at(pos), true)
            }
            is LexResult.Error -> Left(res.error at res.area)
        }
    }
    in '1'..'9' -> when (val res = lexNumber(code, pos + 1, StringBuilder().append(char))) {
        is LexResult.Success -> {
            val nextPos = res.next
            lex(code, nextPos, list + res.value.at(pos), true)
        }
        is LexResult.Error -> Left(res.error at res.area)
    }
    '\'' -> when (val res = lexChar(code, pos + 1)) {
        is LexResult.Success -> {
            val nextPos = res.next
            lex(code, nextPos, list + res.value.at(pos), true)
        }
        is LexResult.Error -> Left(res.error at res.area)
    }
    '\"' -> if (code.getOrNull(pos + 1) == '\"') {
        if (code.getOrNull(pos + 2) == '\"') {
            when (val res = lexMultiLineString(code, pos + 3, emptyList(), StringBuilder())) {
                is LexResult.Success -> {
                    val nextPos = res.next
                    lex(code, nextPos, list + res.value.at(pos), true)
                }
                is LexResult.Error -> Left(res.error at res.area)
            }
        } else {
            lex(code, pos + 2, list + emptyStr.at(pos), true)
        }
    } else {
        when (val res = lexSingleLineString(code, pos + 1, emptyList(), StringBuilder())) {
            is LexResult.Success -> {
                val nextPos = res.next
                lex(code, nextPos, list + res.value.at(pos), true)
            }
            is LexResult.Error -> Left(res.error at res.area)
        }
    }
    in identStartChars -> {
        val (token, nextPos) = lexNormalIdent(code, pos + 1, StringBuilder().append(char))
        lex(code, nextPos, list + token.at(pos), true)
    }
    '`' -> when (val res = lexTickedIdent(code, pos + 1, StringBuilder())) {
        is LexResult.Success -> {
            val nextPos = res.next
            lex(code, nextPos, list + res.value.at(pos), true)
        }
        is LexResult.Error -> Left(res.error at res.area)
    }
    '+' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '+' -> Left(unsupportedOperator("++", "+= 1").at(pos))
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.PlusAssign.at(pos), false)
            } else {
                Left(invalidInfixOp("+=").at(pos))
            }
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                when {
                    isPostfix == isPrefix -> lex(code, pos + 1, list + Token.Plus.at(pos), false)
                    isPrefix -> lex(code, pos + 1, list + Token.UnaryPlus.at(pos), false)
                    else -> Left(unknownPostfixOp("+").at(pos))
                }
            }
        }
    }
    '-' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '>' -> lex(code, pos + 2, list + Token.Arrow.at(pos), false)
            '-' -> Left(unsupportedOperator("--", "-= 1").at(pos))
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.MinusAssign.at(pos), false)
            } else {
                Left(invalidInfixOp("-=").at(pos))
            }
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                when {
                    isPostfix == isPrefix -> lex(code, pos + 1, list + Token.Minus.at(pos), false)
                    isPrefix -> lex(code, pos + 1, list + Token.UnaryMinus.at(pos), false)
                    else -> Left(unknownPostfixOp("-").at(pos))
                }
            }
        }
    }
    '*' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '*' -> Left(unsupportedOperator("*", "pow").at(pos))
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.TimesAssign.at(pos), false)
            } else {
                Left(invalidInfixOp("*=").at(pos))
            }
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                when {
                    isPostfix == isPrefix -> lex(code, pos + 1, list + Token.Times.at(pos), false)
                    isPrefix -> lex(code, pos + 1, list + Token.Spread.at(pos), false)
                    else -> Left(unknownPostfixOp("*").at(pos))
                }
            }
        }
    }
    '/' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '/' -> lex(code, lexSingleLineComment(code, pos + 1), list, false)
            '*' -> lex(code, lexMultiLineComment(code, pos + 1), list, false)
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.DivAssign.at(pos), false)
            } else {
                Left(invalidInfixOp("/=").at(pos))
            }
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                lex(code, pos + 1, list + Token.Div.at(pos), false)
            } else {
                Left(invalidInfixOp("/").at(pos))
            }
        }
    }
    '%' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.RemAssign.at(pos), false)
            } else {
                Left(invalidInfixOp("%=").at(pos))
            }
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                lex(code, pos + 1, list + Token.Rem.at(pos), false)
            } else {
                Left(invalidInfixOp("%").at(pos))
            }
        }
    }
    '?' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '.' -> lex(code, pos + 2, list + Token.SafeAccess.at(pos), false)
            ':' -> if (isOpInfix(code, isPostfix, pos + 2)) {
                lex(code, pos + 2, list + Token.Elvis.at(pos), false)
            } else {
                Left(invalidInfixOp("?:").at(pos))
            }
            else -> lex(code, nextPos, list + Token.QuestionMark.at(pos), false)
        }
    }
    '&' -> {
        val nextPos = pos + 1
        if (code.getOrNull(nextPos) == '&') {
            if (isOpInfix(code, isPostfix, pos + 2)) {
                lex(code, pos + 2, list + Token.And.at(pos), false)
            } else {
                Left(invalidInfixOp("&&").at(pos))
            }
        } else Left(unsupportedOperator("&", "and").at(pos))
    }
    '|' -> {
        val nextPos = pos + 1
        if (code.getOrNull(nextPos) == '|') {
            if (isOpInfix(code, isPostfix, pos + 2)) {
                lex(code, pos + 2, list + Token.Or.at(pos), false)
            } else {
                Left(invalidInfixOp("||").at(pos))
            }
        } else Left(unsupportedOperator("|", "or").at(pos))
    }
    '<' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.LessOrEq.at(pos), false)
            } else {
                Left(invalidInfixOp("<=").at(pos))
            }
            '<' -> Left(unsupportedOperator("<<", "shl").at(pos))
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                lex(code, nextPos, list + Token.Less.at(pos), false)
            } else {
                Left(invalidPrefixOp("<").at(pos))
            }
        }
    }
    '>' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.GreaterOrEq.at(pos), false)
            } else {
                Left(invalidInfixOp(">=").at(pos))
            }
            '>' -> Left(unsupportedOperator(">>", "shr").at(pos))
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                lex(code, nextPos, list + Token.Greater.at(pos), false)
            } else {
                Left(invalidPrefixOp(">").at(pos))
            }
        }
    }
    '!' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> when (code.getOrNull(nextPos + 1)) {
                '=' -> if (isOpInfix(code, isPostfix, nextPos + 2)) {
                    lex(code, nextPos + 2, list + Token.NotRefEq.at(pos + 1), false)
                } else {
                    Left(invalidInfixOp("!==").at(pos + 1))
                }
                else -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                    lex(code, nextPos + 1, list + Token.NotEq.at(pos), false)
                } else {
                    Left(invalidInfixOp("!=").at(pos))
                }
            }
            '!' -> if (isOpPostfix(code, isPostfix, nextPos + 1)) {
                lex(code, nextPos + 1, list + Token.DoubleExclamation.at(pos), false)
            } else {
                Left(invalidPostfixOp("!!").at(pos))
            }
            else -> if (isOpPrefix(code, isPostfix, nextPos)) {
                lex(code, nextPos, list + Token.Not.at(pos), false)
            } else {
                Left(invalidPrefixOp("!").at(pos))
            }
        }
    }
    '=' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> when (code.getOrNull(nextPos + 1)) {
                '=' -> if (isOpInfix(code, isPostfix, nextPos + 2)) {
                    lex(code, nextPos + 2, list + Token.RefEq.at(pos + 1), false)
                } else {
                    Left(invalidInfixOp("===").at(pos + 1))
                }
                else -> if (isOpInfix(code, isPostfix, nextPos + 1)) {
                    lex(code, nextPos + 1, list + Token.Eq.at(pos), false)
                } else {
                    Left(invalidInfixOp("==").at(pos))
                }
            }
            else -> if (isOpInfix(code, isPostfix, nextPos)) {
                lex(code, nextPos, list + Token.Assign.at(pos), false)
            } else {
                Left(invalidInfixOp("=").at(pos))
            }
        }
    }
    else -> Left(errorSymbol(char).at(pos))
}

fun errorSymbol(char: Char) = when (char) {
    '^' -> unsupportedOperator("^", "xor")
    '~' -> unsupportedOperator("^", "inv")
    else -> confusables[char]?.let { (name, actual) ->
        unexpectedConfusableCharacter(char, name, actual, charNames[char] ?: error("Impossible"))
    } ?: unexpectedCharacter(char)
}

fun isOpInfix(code: String, postfix: Boolean, next: StringPos) =
    postfix == code.getOrNull(next).isAfterPrefix()

fun isOpPrefix(code: String, postfix: Boolean, next: StringPos) =
    code.getOrNull(next).isAfterPrefix() && !postfix

fun isOpPostfix(code: String, postfix: Boolean, next: StringPos) =
    postfix && !code.getOrNull(next).isAfterPrefix()

internal tailrec fun lexNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    in '0'..'9' ->
        lexNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexNumber(code, pos + 1, builder)
    '.' -> {
        val next = code.getOrNull(pos + 1)
        if (next in '0'..'9') lexDecimalNumber(code, pos + 2, builder.append(char).append(next))
        else convertIntString(builder) succTo pos
    }
    'b', 'B' ->
        Token.Byte(builder.toString().toByte()) succTo pos + 1
    's', 'S' ->
        Token.Short(builder.toString().toShort()) succTo pos + 1
    'l', 'L' ->
        Token.Long(builder.toString().toLong()) succTo pos + 1
    'f', 'F' ->
        Token.Float(builder.toString().toFloat()) succTo pos + 1
    in identStartChars ->
        invalidDecimalLiteral errAt pos
    else ->
        convertIntString(builder) succTo pos
}

private fun convertIntString(builder: StringBuilder): Token = when {
    builder.length <= 10 -> Token.Int(builder.toString().toInt())
    builder.length <= 19 -> Token.Long(builder.toString().toLong())
    else -> Token.Double(builder.toString().toDouble())
}

private tailrec fun lexDecimalNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalNumber(code, pos + 1, builder.append(char))
    '_' -> lexDecimalNumber(code, pos + 1, builder)
    'e' -> when (val exponentStart = code.getOrNull(pos + 1)) {
        '+', '-' -> {
            val digit = code.getOrNull(pos + 2)
            if (digit?.isDigit() == true)
                lexDecimalExponent(code, pos + 3, builder.append(exponentStart).append(digit))
            else invalidExponent.errAt(pos + 2)
        }
        in '0'..'9' -> lexDecimalExponent(code, pos + 2, builder.append(exponentStart))
        else -> invalidExponent.errAt(pos + 1)
    }
    'f', 'F' ->
        Token.Float(builder.toString().toFloat()) succTo pos + 1
    in identStartChars ->
        invalidDecimalLiteral errAt pos
    else -> Token.Double(builder.toString().toDouble()) succTo pos
}

private tailrec fun lexDecimalExponent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalExponent(code, pos + 1, builder.append(char))
    '_' -> lexDecimalExponent(code, pos + 1, builder)
    in identStartChars -> invalidDecimalLiteral errAt pos
    else -> Token.Double(builder.toString().toDouble()) succTo pos
}

internal tailrec fun lexBinaryNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    '0', '1' ->
        lexBinaryNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexBinaryNumber(code, pos + 1, builder)
    'b', 'B' ->
        Token.Byte(builder.toString().toByte(radix = 2)) succTo pos + 1
    's', 'S' ->
        Token.Short(builder.toString().toShort(radix = 2)) succTo pos + 1
    'l', 'L' ->
        Token.Long(builder.toString().toLong(radix = 2)) succTo pos + 1
    in identStartChars ->
        invalidBinaryLiteral errAt pos
    else -> LexResult.Success(
        if (builder.length <= 32) Token.Int(builder.toString().toInt(radix = 2))
        else Token.Long(builder.toString().toLong(radix = 2)), pos
    )
}

internal tailrec fun lexHexNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> lexHexNumber(code, pos + 1, builder.append(char))
    '_' -> lexHexNumber(code, pos + 1, builder)
    in identStartChars -> invalidHexLiteral errAt pos
    else -> LexResult.Success(
        if (builder.length <= 8) Token.Int(builder.toString().toInt(radix = 16))
        else Token.Long(builder.toString().toLong(radix = 16)), pos
    )
}

internal fun handleEscaped(code: String, pos: StringPos): LexResult<Char> = when (code.getOrNull(pos)) {
    '"' -> '\"' succTo pos + 1
    '$' -> '$' succTo pos + 1
    '\\' -> '\\' succTo pos + 1
    't' -> '\t' succTo pos + 1
    'n' -> '\n' succTo pos + 1
    'b' -> '\b' succTo pos + 1
    'r' -> '\r' succTo pos + 1
    'f' -> 12.toChar() succTo pos + 1
    'v' -> 11.toChar() succTo pos + 1
    'u' -> if (code.getOrNull(pos + 1) == '{') {
        handleUnicode(code, pos + 2)
    } else missingBracketInUnicode errAt pos + 1
    else -> unclosedEscapeCharacter errAt pos
}

private tailrec fun handleUnicode(
    code: String,
    pos: StringPos,
    idBuilder: StringBuilder = StringBuilder()
): LexResult<Char> = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> handleUnicode(code, pos + 1, idBuilder.append(char))
    '}' -> {
        val unicodeStr = idBuilder.toString()
        val length = unicodeStr.length
        when {
            length == 0 -> emptyUnicode.errAt(pos - 1, pos)
            length > 8 -> tooLongUnicode errAt pos
            else -> unicodeStr.toInt().toChar() succTo pos + 1
        }
    }
    else -> invalidHexLiteral errAt pos
}

internal fun lexChar(code: String, pos: StringPos): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    null, '\n' -> loneSingleQuote errAt pos
    '\\' -> when (val res = handleEscaped(code, pos + 1)) {
        is LexResult.Success -> when (code.getOrNull(res.next)) {
            '\'' -> Token.Char(res.value) succTo res.next + 1
            null -> unclosedCharLiteral errAt res.next
            else -> (if (res.value == '\'') missingSingleQuoteOnQuote else missingSingleQuote) errAt res.next
        }
        is LexResult.Error -> res
    }
    else -> {
        val endChar = code.getOrNull(pos + 1)
        when {
            endChar == '\'' -> Token.Char(char) succTo pos + 2
            endChar == null -> unclosedCharLiteral errAt pos + 1
            endChar == ' ' && char == ' ' -> isMalformedTab(code, pos + 2)?.let {
                malformedTab.errAt(pos - 1, it)
            } ?: missingSingleQuote errAt pos + 1
            else -> missingSingleQuote errAt pos + 1
        }
    }
}

private tailrec fun isMalformedTab(code: String, pos: StringPos): StringPos? = when (code.getOrNull(pos)) {
    null -> null
    ' ' -> isMalformedTab(code, pos)
    '\'' -> pos
    else -> null
}