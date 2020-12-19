package com.scientianova.palm.lexer

import com.scientianova.palm.util.ListBuilder
import com.scientianova.palm.util.StringPos

tailrec fun lexFile(
    code: String,
    pos: StringPos = 0,
    streamBuilder: ListBuilder<PToken> = ListBuilder.Null()
): List<PToken> {
    return lexFile(
        code, pos, streamBuilder + when (val char = code.getOrNull(pos)) {
            null -> return streamBuilder.buildList()
            '\n' -> Token.EOL to pos + 1
            '\t', ' ', '\r' -> lexWhitespace(code, pos + 1)
            '#' -> lexRawString(code, pos + 1)
            '(' -> lexNested(code, ')', pos + 1)
            '[' -> lexNested(code, ']', pos + 1)
            '{' -> lexNested(code, '}', pos + 1)
            ')' -> Token.Error("Unexpected character") to pos + 1
            ']' -> Token.Error("Unexpected character") to pos + 1
            '}' -> Token.Error("Unexpected character") to pos + 1
            ',' -> Token.Comma to pos + 1
            ';' -> Token.Semicolon to pos + 1
            '@' -> Token.At to pos + 1
            '?' -> Token.QuestionMark to pos + 1
            ':' -> if (code.getOrNull(pos + 1) == ':') {
                Token.DoubleColon to pos + 2
            } else {
                Token.Colon to pos + 1
            }
            '.' -> if (code.getOrNull(pos + 1) == '.') {
                Token.RangeTo to pos + 2
            } else {
                Token.Dot to pos + 1
            }
            '0' -> when (code.getOrNull(pos + 1)) {
                'b', 'B' -> lexBinaryNumber(code, pos + 2, StringBuilder())
                'x', 'X' -> lexHexNumber(code, pos + 2, StringBuilder())
                else -> lexNumber(code, pos + 1, StringBuilder("0"))
            }
            in '1'..'9' -> lexNumber(code, pos + 1, StringBuilder().append(char))
            '\'' -> lexChar(code, pos + 1)
            '\"' -> lexString(code, pos + 1, emptyList(), StringBuilder(), 0)
            '`' -> lexTickedIdent(code, pos + 1, StringBuilder())
            '/' -> when (code.getOrNull(pos + 1)) {
                '/' -> lexSingleLineComment(code, pos + 2)
                '*' -> lexMultiLineComment(code, pos + 2)
                else -> lexSymbol(code, pos + 1, '/')
            }
            in identStartChars -> lexNormalIdent(code, pos + 1, StringBuilder().append(char))
            in symbolChars -> lexSymbol(code, pos + 1, char)
            in confusables -> {
                val confusable = confusables[char]!!
                Token.Error("Found a ${confusable.first}, which looks like a ${charNames[confusable.second]}") to pos + 1
            }
            else -> Token.Error("Unsupported character") to pos + 1
        }
    )
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
internal tailrec fun lexNested(
    code: String,
    endDelim: Char,
    pos: StringPos = 0,
    streamBuilder: ListBuilder<PToken> = ListBuilder.Null()
): PToken {
    return lexNested(
        code, endDelim, pos, streamBuilder + when (val char = code.getOrNull(pos)) {
            null -> return Token.Error("Missing $endDelim") to pos
            '\n' -> Token.EOL to pos + 1
            '\t', ' ', '\r' -> lexWhitespace(code, pos + 1)
            '#' -> lexRawString(code, pos + 1)
            '(' -> lexNested(code, ')', pos + 1)
            '[' -> lexNested(code, ']', pos + 1)
            '{' -> lexNested(code, '}', pos + 1)
            ')' -> if (endDelim == ')') {
                return Token.Parens(streamBuilder.buildList()) to pos + 1
            } else {
                Token.Error("Unexpected character") to pos + 1
            }
            ']' -> if (endDelim == ']') {
                return Token.Brackets(streamBuilder.buildList()) to pos + 1
            } else {
                Token.Error("Unexpected character") to pos + 1
            }
            '}' -> if (endDelim == '}') {
                return Token.Braces(streamBuilder.buildList()) to pos + 1
            } else {
                Token.Error("Unexpected character") to pos + 1
            }
            ',' -> Token.Comma to pos + 1
            ';' -> Token.Semicolon to pos + 1
            '@' -> Token.At to pos + 1
            '?' -> Token.QuestionMark to pos + 1
            ':' -> if (code.getOrNull(pos + 1) == ':') {
                Token.DoubleColon to pos + 2
            } else {
                Token.Colon to pos + 1
            }
            '.' -> if (code.getOrNull(pos + 1) == '.') {
                Token.RangeTo to pos + 2
            } else {
                Token.Dot to pos + 1
            }
            '0' -> when (code.getOrNull(pos + 1)) {
                'b', 'B' -> lexBinaryNumber(code, pos + 2, StringBuilder())
                'x', 'X' -> lexHexNumber(code, pos + 2, StringBuilder())
                else -> lexNumber(code, pos + 1, StringBuilder("0"))
            }
            in '1'..'9' -> lexNumber(code, pos + 1, StringBuilder().append(char))
            '\'' -> lexChar(code, pos + 1)
            '\"' -> lexString(code, pos + 1, emptyList(), StringBuilder(), 0)
            '`' -> lexTickedIdent(code, pos + 1, StringBuilder())
            '/' -> when (code.getOrNull(pos + 1)) {
                '/' -> lexSingleLineComment(code, pos + 2)
                '*' -> lexMultiLineComment(code, pos + 2)
                else -> lexSymbol(code, pos + 1, '/')
            }
            in identStartChars -> lexNormalIdent(code, pos + 1, StringBuilder().append(char))
            in symbolChars -> lexSymbol(code, pos + 1, char)
            in confusables -> {
                val confusable = confusables[char]!!
                Token.Error("Found a ${confusable.first}, which looks like a ${charNames[confusable.second]}") to pos + 1
            }
            else -> Token.Error("Unsupported character") to pos + 1
        }
    )
}

private tailrec fun lexRawString(code: String, pos: StringPos, hashes: Int = 1): PToken = when (code.getOrNull(pos)) {
    '#' -> lexRawString(code, pos + 1, hashes + 1)
    '"' -> lexString(code, pos, emptyList(), StringBuilder(), hashes)
    else -> Token.Error("Expected a double colon") to pos
}

private fun lexSymbolChars(code: String, pos: StringPos, builder: StringBuilder): StringBuilder =
    when (val char = code.getOrNull(pos)) {
        in symbolChars -> lexSymbolChars(code, pos + 1, builder.append(char))
        else -> builder
    }

private fun lexSymbol(code: String, pos: StringPos, first: Char) =
    when (val symbol = lexSymbolChars(code, pos, StringBuilder().append(first)).toString()) {
        "+" -> Token.Plus to pos + 1
        "-" -> Token.Minus to pos + 1
        "*" -> Token.Times to pos + 1
        "/" -> Token.Div to pos + 1
        "%" -> Token.Rem to pos + 1
        "&&" -> Token.And to pos + 2
        "||" -> Token.Or to pos + 2
        "!" -> Token.ExclamationMark to pos + 1
        "==" -> Token.Eq to pos + 2
        "!=" -> Token.NotEq to pos + 2
        "===" -> Token.RefEq to pos + 3
        "!==" -> Token.NotRefEq to pos + 3
        "=" -> Token.Assign to pos + 1
        "+=" -> Token.PlusAssign to pos + 2
        "-=" -> Token.MinusAssign to pos + 2
        "*=" -> Token.TimesAssign to pos + 2
        "/=" -> Token.DivAssign to pos + 2
        "%=" -> Token.RemAssign to pos + 2
        "->" -> Token.Arrow to pos + 1
        ">" -> Token.Greater to pos + 1
        "<" -> Token.Less to pos + 1
        ">=" -> Token.GreaterOrEq to pos + 2
        "<=" -> Token.GreaterOrEq to pos + 2
        else -> Token.Error("Unknown operator") to pos + symbol.length
    }

internal tailrec fun lexNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' ->
        lexNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexNumber(code, pos + 1, builder)
    '.' -> {
        val next = code.getOrNull(pos + 1)
        if (next in '0'..'9') lexDecimalNumber(code, pos + 2, builder.append(char).append(next))
        else convertIntString(builder) to pos
    }
    'b', 'B' ->
        Token.Byte(builder.toString().toByte()) to pos + 1
    's', 'S' ->
        Token.Short(builder.toString().toShort()) to pos + 1
    'l', 'L' ->
        Token.Long(builder.toString().toLong()) to pos + 1
    'f', 'F' ->
        Token.Float(builder.toString().toFloat()) to pos + 1
    in identStartChars ->
        Token.Error("invalid literal") to pos + 1
    else ->
        convertIntString(builder) to pos
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
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalNumber(code, pos + 1, builder.append(char))
    '_' -> lexDecimalNumber(code, pos + 1, builder)
    'e' -> when (val exponentStart = code.getOrNull(pos + 1)) {
        '+', '-' -> {
            val digit = code.getOrNull(pos + 2)
            if (digit?.isDigit() == true)
                lexDecimalExponent(code, pos + 3, builder.append(exponentStart).append(digit))
            else Token.Error("Invalid exponent") to pos + 2
        }
        in '0'..'9' -> lexDecimalExponent(code, pos + 2, builder.append(exponentStart))
        else -> Token.Error("Invalid exponent") to pos + 1
    }
    'f', 'F' -> Token.Float(builder.toString().toFloat()) to pos + 1
    in identStartChars -> Token.Error("Invalid literal") to pos + 1
    else -> Token.Double(builder.toString().toDouble()) to pos + 1
}

private tailrec fun lexDecimalExponent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalExponent(code, pos + 1, builder.append(char))
    '_' -> lexDecimalExponent(code, pos + 1, builder)
    in identStartChars -> Token.Error("Invalid literal") to pos + 1
    else -> Token.Double(builder.toString().toDouble()) to pos
}

internal tailrec fun lexBinaryNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    '0', '1' ->
        lexBinaryNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexBinaryNumber(code, pos + 1, builder)
    'b', 'B' ->
        Token.Byte(builder.toString().toByte(radix = 2)) to pos + 1
    's', 'S' ->
        Token.Short(builder.toString().toShort(radix = 2)) to pos + 1
    'l', 'L' ->
        Token.Long(builder.toString().toLong(radix = 2)) to pos + 1
    in identStartChars -> Token.Error("Invalid literal") to pos + 1
    else -> (
            if (builder.length <= 32) Token.Int(builder.toString().toInt(radix = 2))
            else Token.Long(builder.toString().toLong(radix = 2))) to pos + 1
}

internal tailrec fun lexHexNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> lexHexNumber(code, pos + 1, builder.append(char))
    '_' -> lexHexNumber(code, pos + 1, builder)
    in identStartChars -> Token.Error("Invalid literal") to pos + 1
    else -> (
            if (builder.length <= 8) Token.Int(builder.toString().toInt(radix = 16))
            else Token.Long(builder.toString().toLong(radix = 16))) to pos
}

internal fun handleEscaped(code: String, pos: StringPos, hashes: Int): LexResult<Char> =
    if (hashNumEq(code, pos + 1, hashes)) {
        val afterHashes = pos + hashes
        when (code.getOrNull(afterHashes)) {
            '"' -> '\"' succTo afterHashes + 1
            '$' -> '$' succTo afterHashes + 1
            '\\' -> '\\' succTo afterHashes + 1
            't' -> '\t' succTo afterHashes + 1
            'n' -> '\n' succTo afterHashes + 1
            'b' -> '\b' succTo afterHashes + 1
            'r' -> '\r' succTo afterHashes + 1
            'f' -> 12.toChar() succTo afterHashes + 1
            'v' -> 11.toChar() succTo afterHashes + 1
            'u' -> if (code.getOrNull(afterHashes + 1) == '{') {
                handleUnicode(code, afterHashes + 2)
            } else "Missing bracket after unicode".errAt(afterHashes + 1)
            else -> "Unclosed escape character".errAt(afterHashes)
        }
    } else {
        code.getOrNull(pos)?.succTo(pos + 1) ?: "Unclosed escape character".errAt(pos)
    }

internal fun hashNumEq(code: String, pos: StringPos, expected: Int) =
    expected == 0 || (code.length >= pos + expected && pos.until(pos + expected).all { code[it] == '#' })

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
            length == 0 -> "Empty unicode".errAt(pos - 1, pos)
            length > 8 -> "Unicode too long".errAt(pos)
            else -> unicodeStr.toInt().toChar() succTo pos + 1
        }
    }
    else -> "Invalid hex literal".errAt(pos)
}

internal fun lexChar(code: String, pos: StringPos): PToken = when (val char = code.getOrNull(pos)) {
    null, '\n' -> Token.Error("Lone single quote") to pos + 1
    '\\' -> when (val res = handleEscaped(code, pos + 1, 0)) {
        is LexResult.Success -> when (code.getOrNull(res.next)) {
            '\'' -> Token.Char(res.value) to res.next + 1
            null -> Token.Error("Unclosed char literal") to res.next + 1
            else -> Token.Error("Missing single quote") to res.next + 1
        }
        is LexResult.Error -> Token.Error(res.error) to res.next
    }
    else -> {
        val endChar = code.getOrNull(pos + 1)
        when {
            endChar == '\'' -> Token.Char(char) to pos + 2
            endChar == null -> Token.Error("Unclosed char literal") to pos + 2
            endChar == ' ' && char == ' ' -> isMalformedTab(code, pos + 2)?.let {
                Token.Error("Malformed tab") to it + 1
            } ?: Token.Error("Missing single quote") to pos + 2
            else -> Token.Error("Missing single quote") to pos + 2
        }
    }
}

private tailrec fun isMalformedTab(code: String, pos: StringPos): StringPos? = when (code.getOrNull(pos)) {
    null -> null
    ' ' -> isMalformedTab(code, pos)
    '\'' -> pos
    else -> null
}