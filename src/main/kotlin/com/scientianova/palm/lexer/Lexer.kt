package com.scientianova.palm.lexer

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun lex(
    code: String,
    pos: StringPos
): PToken = when (val char = code.getOrNull(pos)) {
    null -> Token.EOF.at(pos)
    '\n' -> Token.EOL.at(pos)
    '\t', ' ', '\r' -> lexWhitespace(code, pos, pos + 1)
    '(' -> Token.LParen.at(pos)
    '[' -> Token.LBracket.at(pos)
    '{' -> Token.LBrace.at(pos)
    ')' -> Token.RParen.at(pos)
    ']' -> Token.RBracket.at(pos)
    '}' -> Token.RBrace.at(pos)
    ',' -> Token.Comma.at(pos)
    ';' -> Token.Semicolon.at(pos)
    '@' -> Token.At.at(pos)
    ':' -> if (code.getOrNull(pos + 1) == ':') {
        Token.DoubleColon.at(pos, pos + 2)
    } else {
        Token.Colon.at(pos)
    }
    '.' -> if (code.getOrNull(pos + 1) == '.') {
        val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
        val isPrefix = code.getOrNull(pos + 2).isAfterPrefix()
        when {
            isPostfix == isPrefix -> Token.RangeTo.at(pos, pos + 2)
            isPrefix -> Token.RangeFrom.at(pos, pos + 2)
            else -> Token.RangeUntil.at(pos, pos + 2)
        }
    } else {
        Token.Dot.at(pos)
    }
    '0' -> when (code.getOrNull(pos + 1)) {
        'b', 'B' -> lexBinaryNumber(code, pos, pos + 2, StringBuilder())
        'x', 'X' -> lexHexNumber(code, pos, pos + 2, StringBuilder())
        else -> lexNumber(code, pos, pos + 1, StringBuilder("0"))
    }
    in '1'..'9' -> lexNumber(code, pos, pos + 1, StringBuilder().append(char))
    '\'' -> lexChar(code, pos, pos + 1)
    '\"' -> if (code.getOrNull(pos + 1) == '\"') {
        if (code.getOrNull(pos + 2) == '\"') {
            lexMultiLineString(code, pos, pos + 3, emptyList(), StringBuilder())
        } else {
            emptyStr.at(pos, pos + 2)
        }
    } else {
        lexSingleLineString(code, pos, pos + 1, emptyList(), StringBuilder())
    }
    in identStartChars -> lexNormalIdent(code, pos, pos + 1, StringBuilder().append(char))
    '`' -> lexTickedIdent(code, pos, pos + 1, StringBuilder())
    '+' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '+' -> unsupportedOperator("++", "+= 1").token(pos, pos + 2)
            '=' -> infixOp(code, pos, nextPos + 1, Token.PlusAssign, "+=")
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
                when {
                    isPostfix == isPrefix -> Token.Plus.at(pos)
                    isPrefix -> Token.UnaryPlus.at(pos)
                    else -> unknownPostfixOp("+").token(pos)
                }
            }
        }
    }
    '-' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '>' -> Token.Arrow.at(pos, pos + 2)
            '-' -> unsupportedOperator("--", "-= 1").token(pos, pos + 2)
            '=' -> infixOp(code, pos, nextPos + 1, Token.MinusAssign, "-=")
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
                when {
                    isPostfix == isPrefix -> Token.Minus.at(pos)
                    isPrefix -> Token.UnaryMinus.at(pos)
                    else -> unknownPostfixOp("-").token(pos)
                }
            }
        }
    }
    '*' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '*' -> unsupportedOperator("*", "pow").token(pos, pos + 2)
            '=' -> infixOp(code, pos, nextPos + 1, Token.TimesAssign, "*=")
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
                when {
                    isPostfix == isPrefix -> Token.Times.at(pos)
                    isPrefix -> Token.Spread.at(pos)
                    else -> unknownPostfixOp("*").token(pos)
                }
            }
        }
    }
    '/' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '/' -> lexSingleLineComment(code, nextPos + 1)
            '*' -> lexMultiLineComment(code, pos, pos + 1)
            '=' -> infixOp(code, pos, nextPos + 1, Token.DivAssign, "/=")
            else -> infixOp(code, pos, nextPos, Token.Div, "/")
        }
    }
    '%' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> infixOp(code, pos, nextPos + 1, Token.RemAssign, "%=")
            else -> infixOp(code, pos, nextPos, Token.Rem, "%")
        }
    }
    '?' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '.' -> Token.SafeAccess.at(pos)
            ':' -> postfixOp(code, pos, nextPos + 1, Token.Elvis, "?:")
            else -> Token.QuestionMark.at(pos)
        }
    }
    '&' -> if (code.getOrNull(pos + 1) == '&') {
        infixOp(code, pos, pos + 2, Token.And, "&&")
    } else {
        unsupportedOperator("&", "and").token(pos)
    }
    '|' -> if (code.getOrNull(pos + 1) == '|') {
        infixOp(code, pos, pos + 2, Token.Or, "||")
    } else {
        unsupportedOperator("|", "or").token(pos)
    }
    '<' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> infixOp(code, pos, nextPos + 1, Token.LessOrEq, "<=")
            '<' -> unsupportedOperator("<<", "shl").token(pos, pos + 2)
            else -> infixOp(code, pos, nextPos, Token.Less, "<")
        }
    }
    '>' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> infixOp(code, pos, nextPos + 1, Token.GreaterOrEq, ">=")
            '<' -> unsupportedOperator(">>", "shr").token(pos, pos + 2)
            else -> infixOp(code, pos, nextPos, Token.Greater, ">")
        }
    }
    '!' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> when (code.getOrNull(nextPos + 1)) {
                '=' -> infixOp(code, pos, nextPos + 2, Token.NotRefEq, "!==")
                else -> infixOp(code, pos, nextPos + 1, Token.NotEq, "!=")
            }
            '!' -> postfixOp(code, pos, nextPos + 1, Token.NonNull, "!!")
            else -> prefixOp(code, pos, nextPos, Token.Not, "!")
        }
    }
    '=' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '=' -> when (code.getOrNull(nextPos + 1)) {
                '=' -> infixOp(code, pos, nextPos + 2, Token.RefEq, "===")
                else -> infixOp(code, pos, nextPos + 1, Token.Eq, "==")
            }
            else -> infixOp(code, pos, nextPos, Token.Assign, "=")
        }
    }
    else -> errorSymbol(char).token(pos)
}

fun errorSymbol(char: Char) = when (char) {
    '^' -> unsupportedOperator("^", "xor")
    '~' -> unsupportedOperator("^", "inv")
    else -> confusables[char]?.let { (name, actual) ->
        unexpectedConfusableCharacter(char, name, actual, charNames[char] ?: error("Impossible"))
    } ?: unexpectedCharacter(char)
}

fun infixOp(code: String, start: StringPos, next: StringPos, op: Token, symbol: String) =
    if (code.getOrNull(start - 1).isBeforePostfix() == code.getOrNull(next).isAfterPrefix()) {
        op
    } else {
        Token.Error(invalidInfixOp(symbol))
    }.at(start, next)

fun prefixOp(code: String, start: StringPos, next: StringPos, op: Token, symbol: String) =
    if (code.getOrNull(next).isAfterPrefix() && !code.getOrNull(start - 1).isBeforePostfix()) {
        op
    } else {
        Token.Error(invalidInfixOp(symbol))
    }.at(start, next)

fun postfixOp(code: String, start: StringPos, next: StringPos, op: Token, symbol: String) =
    if (code.getOrNull(start - 1).isBeforePostfix() && !code.getOrNull(next).isAfterPrefix()) {
        op
    } else {
        Token.Error(invalidInfixOp(symbol))
    }.at(start, next)

internal tailrec fun lexNumber(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' ->
        lexNumber(code, pos + 1, start, builder.append(char))
    '_' ->
        lexNumber(code, pos + 1, start, builder)
    '.' -> {
        val next = code.getOrNull(pos + 1)
        if (next in '0'..'9') lexDecimalNumber(code, start, pos + 2, builder.append(char).append(next))
        else convertIntString(builder).at(start, pos)
    }
    'b', 'B' ->
        Token.Byte(builder.toString().toByte()).at(start, pos + 1)
    's', 'S' ->
        Token.Short(builder.toString().toShort()).at(start, pos + 1)
    'l', 'L' ->
        Token.Long(builder.toString().toLong()).at(start, pos + 1)
    'f', 'F' ->
        Token.Float(builder.toString().toFloat()).at(start, pos + 1)
    in identStartChars ->
        invalidDecimalLiteral.token(pos)
    else ->
        convertIntString(builder).at(start, pos)
}

private fun convertIntString(builder: StringBuilder): Token = when {
    builder.length <= 10 -> Token.Int(builder.toString().toInt())
    builder.length <= 19 -> Token.Long(builder.toString().toLong())
    else -> Token.Double(builder.toString().toDouble())
}

private tailrec fun lexDecimalNumber(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalNumber(code, start, pos + 1, builder.append(char))
    '_' -> lexDecimalNumber(code, start, pos + 1, builder)
    'e' -> when (val exponentStart = code.getOrNull(pos + 1)) {
        '+', '-' -> {
            val digit = code.getOrNull(pos + 2)
            if (digit?.isDigit() == true)
                lexDecimalExponent(code, start, pos + 3, builder.append(exponentStart).append(digit))
            else invalidExponent.token(pos + 2)
        }
        in '0'..'9' -> lexDecimalExponent(code, start, pos + 2, builder.append(exponentStart))
        else -> invalidExponent.token(pos + 1)
    }
    'f', 'F' -> Token.Float(builder.toString().toFloat()).at(start, pos + 1)
    in identStartChars -> invalidDecimalLiteral.token( pos)
    else -> Token.Double(builder.toString().toDouble()).at(start, pos + 1)
}

private tailrec fun lexDecimalExponent(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalExponent(code, start, pos + 1, builder.append(char))
    '_' -> lexDecimalExponent(code, start, pos + 1, builder)
    in identStartChars -> invalidDecimalLiteral.token(pos)
    else -> Token.Double(builder.toString().toDouble()).at(start, pos)
}

internal tailrec fun lexBinaryNumber(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    '0', '1' ->
        lexBinaryNumber(code, start, pos + 1, builder.append(char))
    '_' ->
        lexBinaryNumber(code, start, pos + 1, builder)
    'b', 'B' ->
        Token.Byte(builder.toString().toByte(radix = 2)).at(start, pos + 1)
    's', 'S' ->
        Token.Short(builder.toString().toShort(radix = 2)).at(start, pos + 1)
    'l', 'L' ->
        Token.Long(builder.toString().toLong(radix = 2)).at(start, pos + 1)
    in identStartChars -> invalidBinaryLiteral.token(pos)
    else -> Positioned(
        if (builder.length <= 32) Token.Int(builder.toString().toInt(radix = 2))
        else Token.Long(builder.toString().toLong(radix = 2)), start, pos
    )
}

internal tailrec fun lexHexNumber(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> lexHexNumber(code, start, pos + 1, builder.append(char))
    '_' -> lexHexNumber(code, start, pos + 1, builder)
    in identStartChars -> invalidHexLiteral.token(pos)
    else -> Positioned(
        if (builder.length <= 8) Token.Int(builder.toString().toInt(radix = 16))
        else Token.Long(builder.toString().toLong(radix = 16)), start, pos
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
    } else missingBracketInUnicode.errAt(pos + 1)
    else -> unclosedEscapeCharacter.errAt(pos)
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
            length > 8 -> tooLongUnicode.errAt(pos)
            else -> unicodeStr.toInt().toChar() succTo pos + 1
        }
    }
    else -> invalidHexLiteral.errAt(pos)
}

internal fun lexChar(code: String, start: StringPos, pos: StringPos): PToken = when (val char = code.getOrNull(pos)) {
    null, '\n' -> loneSingleQuote.token(pos)
    '\\' -> when (val res = handleEscaped(code, pos + 1)) {
        is LexResult.Success -> when (code.getOrNull(res.next)) {
            '\'' -> Token.Char(res.value).at(start, res.next + 1)
            null -> unclosedCharLiteral.token(res.next)
            else -> (if (res.value == '\'') missingSingleQuoteOnQuote else missingSingleQuote).token(res.next)
        }
        is LexResult.Error -> res.error.token(res.start, res.next)
    }
    else -> {
        val endChar = code.getOrNull(pos + 1)
        when {
            endChar == '\'' -> Token.Char(char).at(start, pos + 2)
            endChar == null -> unclosedCharLiteral.token(pos + 1)
            endChar == ' ' && char == ' ' -> isMalformedTab(code, pos + 2)?.let {
                malformedTab.token(pos - 1, it)
            } ?: missingSingleQuote.token(pos + 1)
            else -> missingSingleQuote.token(pos + 1)
        }
    }
}

private tailrec fun isMalformedTab(code: String, pos: StringPos): StringPos? = when (code.getOrNull(pos)) {
    null -> null
    ' ' -> isMalformedTab(code, pos)
    '\'' -> pos
    else -> null
}