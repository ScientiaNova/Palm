package com.scientianova.palm.lexer

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.StringPos

tailrec fun lex(
    code: String,
    pos: StringPos
): TokenData = when (val char = code.getOrNull(pos)) {
    null -> Token.EOF.data(pos)
    '\n' -> Token.EOL.data(pos)
    '\t', ' ', '\r' -> lex(code, pos + 1)
    '(' -> Token.LParen.data(pos)
    '[' -> Token.LBracket.data(pos)
    '{' -> Token.LBrace.data(pos)
    ')' -> Token.RParen.data(pos)
    ']' -> Token.RBracket.data(pos)
    '}' -> Token.RBrace.data(pos)
    ',' -> Token.Comma.data(pos)
    ';' -> Token.Semicolon.data(pos)
    '@' -> Token.At.data(pos)
    ':' -> if (code.getOrNull(pos + 1) == ':') {
        Token.DoubleColon.data(pos, pos + 2)
    } else {
        Token.Colon.data(pos)
    }
    '.' -> if (code.getOrNull(pos + 1) == '.') {
        val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
        val isPrefix = code.getOrNull(pos + 2).isAfterPrefix()
        when {
            isPostfix == isPrefix -> Token.RangeTo.data(pos, pos + 2)
            isPrefix -> Token.RangeFrom.data(pos, pos + 2)
            else -> Token.RangeUntil.data(pos, pos + 2)
        }
    } else {
        Token.Dot.data(pos)
    }
    '0' -> when (code.getOrNull(pos + 1)) {
        'b', 'B' -> lexBinaryNumber(code, pos + 2, StringBuilder())
        'x', 'X' -> lexHexNumber(code, pos + 2, StringBuilder())
        else -> lexNumber(code, pos + 1, StringBuilder("0"))
    }.tokenData(pos)
    in '1'..'9' -> lexNumber(code, pos + 1, StringBuilder().append(char)).tokenData(pos)
    '\'' -> lexChar(code, pos + 1).tokenData(pos)
    '\"' -> if (code.getOrNull(pos + 1) == '\"') {
        if (code.getOrNull(pos + 2) == '\"') {
            lexMultiLineString(code, pos + 3, emptyList(), StringBuilder()).tokenData(pos)
        } else {
            emptyStr.data(pos, pos + 2)
        }
    } else {
        lexSingleLineString(code, pos + 1, emptyList(), StringBuilder()).tokenData(pos)
    }
    in identStartChars -> {
        val (token, nextPos) = lexNormalIdent(code, pos + 1, StringBuilder().append(char))
        token.data(pos, nextPos)
    }
    '`' -> lexTickedIdent(code, pos + 1, StringBuilder()).tokenData(pos)
    '+' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '+' -> unsupportedOperator("++", "+= 1").token(pos, pos + 2)
            '=' -> infixOp(code, pos, nextPos + 1, Token.PlusAssign, "+=")
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
                when {
                    isPostfix == isPrefix -> Token.Plus.data(pos)
                    isPrefix -> Token.UnaryPlus.data(pos)
                    else -> unknownPostfixOp("+").token(pos)
                }
            }
        }
    }
    '-' -> {
        val nextPos = pos + 1
        when (val nextChar = code.getOrNull(nextPos)) {
            '>' -> Token.Arrow.data(pos, pos + 2)
            '-' -> unsupportedOperator("--", "-= 1").token(pos, pos + 2)
            '=' -> infixOp(code, pos, nextPos + 1, Token.MinusAssign, "-=")
            else -> {
                val isPrefix = nextChar.isAfterPrefix()
                val isPostfix = code.getOrNull(pos - 1).isBeforePostfix()
                when {
                    isPostfix == isPrefix -> Token.Minus.data(pos)
                    isPrefix -> Token.UnaryMinus.data(pos)
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
                    isPostfix == isPrefix -> Token.Times.data(pos)
                    isPrefix -> Token.Spread.data(pos)
                    else -> unknownPostfixOp("*").token(pos)
                }
            }
        }
    }
    '/' -> {
        val nextPos = pos + 1
        when (code.getOrNull(nextPos)) {
            '/' -> lex(code, lexSingleLineComment(code, pos + 1))
            '*' -> lex(code, lexMultiLineComment(code, pos + 1))
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
            '.' -> Token.SafeAccess.data(pos)
            ':' -> postfixOp(code, pos, nextPos + 1, Token.Elvis, "?:")
            else -> Token.QuestionMark.data(pos)
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
            '!' -> postfixOp(code, pos, nextPos + 1, Token.DoubleExclamation, "!!")
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

fun isOpInfix(code: String, prev: StringPos, next: StringPos) =
    code.getOrNull(prev).isBeforePostfix() == code.getOrNull(next).isAfterPrefix()

fun infixOp(code: String, start: StringPos, next: StringPos, op: Token, symbol: String) =
    if (isOpInfix(code, start - 1, next)) {
        op
    } else {
        Token.Error(invalidInfixOp(symbol))
    }.data(start, next)

fun isOpPrefix(code: String, prev: StringPos, next: StringPos) =
    code.getOrNull(next).isAfterPrefix() && !code.getOrNull(prev).isBeforePostfix()

fun prefixOp(code: String, start: StringPos, next: StringPos, op: Token, symbol: String) =
    if (isOpPrefix(code, start - 1, next)) {
        op
    } else {
        Token.Error(invalidInfixOp(symbol))
    }.data(start, next)

fun isOpPostfix(code: String, prev: StringPos, next: StringPos) =
    code.getOrNull(prev).isBeforePostfix() && !code.getOrNull(next).isAfterPrefix()

fun postfixOp(code: String, start: StringPos, next: StringPos, op: Token, symbol: String) =
    if (isOpPostfix(code, start - 1, next)) {
        op
    } else {
        Token.Error(invalidInfixOp(symbol))
    }.data(start, next)

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
        invalidDecimalLiteral.errAt(pos)
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
    'f', 'F' -> Token.Float(builder.toString().toFloat()) succTo pos + 1
    in identStartChars -> invalidDecimalLiteral.errAt(pos)
    else -> Token.Double(builder.toString().toDouble()) succTo pos
}

private tailrec fun lexDecimalExponent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalExponent(code, pos + 1, builder.append(char))
    '_' -> lexDecimalExponent(code, pos + 1, builder)
    in identStartChars -> invalidDecimalLiteral.errAt(pos)
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
    in identStartChars -> invalidBinaryLiteral.errAt(pos)
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
    in identStartChars -> invalidHexLiteral.errAt(pos)
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

internal fun lexChar(code: String, pos: StringPos): LexResult<Token> = when (val char = code.getOrNull(pos)) {
    null, '\n' -> loneSingleQuote.errAt(pos)
    '\\' -> when (val res = handleEscaped(code, pos + 1)) {
        is LexResult.Success -> when (code.getOrNull(res.next)) {
            '\'' -> Token.Char(res.value) succTo res.next + 1
            null -> unclosedCharLiteral.errAt(res.next)
            else -> (if (res.value == '\'') missingSingleQuoteOnQuote else missingSingleQuote).errAt(res.next)
        }
        is LexResult.Error -> res
    }
    else -> {
        val endChar = code.getOrNull(pos + 1)
        when {
            endChar == '\'' -> Token.Char(char) succTo pos + 2
            endChar == null -> unclosedCharLiteral.errAt(pos + 1)
            endChar == ' ' && char == ' ' -> isMalformedTab(code, pos + 2)?.let {
                malformedTab.errAt(pos - 1, it)
            } ?: missingSingleQuote.errAt(pos + 1)
            else -> missingSingleQuote.errAt(pos + 1)
        }
    }
}

private tailrec fun isMalformedTab(code: String, pos: StringPos): StringPos? = when (code.getOrNull(pos)) {
    null -> null
    ' ' -> isMalformedTab(code, pos)
    '\'' -> pos
    else -> null
}