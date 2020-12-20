package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

tailrec fun Lexer.lexFile(code: String): Lexer {
    return when (val char = code.getOrNull(pos)) {
        null -> return this
        '\n' -> Token.EOL.add(pos + 1)
        '\t', ' ', '\r' -> lexWhitespace(code, pos + 1).add()
        '#' -> lexRawString(code, pos + 1)
        '(' -> {
            val nested = Lexer(pos + 1).lexNested(code, ')')
            syncErrors(nested).apply {
                Token.Parens(nested.tokens.buildList()).add(nested.pos)
            }
        }
        '[' -> {
            val nested = Lexer(pos + 1).lexNested(code, ']')
            syncErrors(nested).apply {
                Token.Brackets(nested.tokens.buildList()).add(nested.pos)
            }
        }
        '{' -> {
            val nested = Lexer(pos + 1).lexNested(code, '}')
            syncErrors(nested).apply {
                Token.Braces(nested.tokens.buildList()).add(nested.pos)
            }
        }
        ')' -> err("Unexpected character", pos)
        ']' -> err("Unexpected character", pos)
        '}' -> err("Unexpected character", pos)
        ',' -> Token.Comma.add(pos + 1)
        ';' -> Token.Semicolon.add(pos + 1)
        '@' -> Token.At.add(pos + 1)
        '?' -> Token.QuestionMark.add(pos + 1)
        ':' -> if (code.getOrNull(pos + 1) == ':') {
            Token.DoubleColon.add(pos + 2)
        } else {
            Token.Colon.add(pos + 1)
        }
        '.' -> if (code.getOrNull(pos + 1) == '.') {
            Token.RangeTo.add(pos + 2)
        } else {
            Token.Dot.add(pos + 1)
        }
        '0' -> when (code.getOrNull(pos + 1)) {
            'b', 'B' -> lexBinaryNumber(code, pos + 2, StringBuilder()).add()
            'o', 'O' -> lexOctalNumber(code, pos + 2, StringBuilder()).add()
            'z', 'Z' -> lexDozenalNumber(code, pos + 2, StringBuilder()).add()
            'x', 'X' -> lexHexNumber(code, pos + 2, StringBuilder()).add()
            else -> lexNumber(code, pos + 1, StringBuilder("0"))
        }
        in '1'..'9' -> lexNumber(code, pos + 1, StringBuilder().append(char))
        '\'' -> lexChar(code, pos + 1)
        '\"' -> lexString(code, pos + 1, emptyList(), StringBuilder(), 0)
        '`' -> lexTickedIdent(code, pos + 1, StringBuilder())
        '/' -> when (code.getOrNull(pos + 1)) {
            '/' -> lexSingleLineComment(code, pos + 2).add()
            '*' -> lexMultiLineComment(code, pos + 2)
            else -> lexSymbol(code, pos + 1, '/')
        }
        in identStartChars -> lexNormalIdent(code, pos + 1, StringBuilder().append(char)).add()
        in symbolChars -> lexSymbol(code, pos + 1, char)
        in confusables -> {
            val confusable = confusables[char]!!
            Token.Error("Found a ${confusable.first}, which looks like a ${charNames[confusable.second]}").add(pos + 1)
        }
        else -> Token.Error("Unsupported character").add(pos + 1)
    }.lexFile(code)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
internal tailrec fun Lexer.lexNested(
    code: String,
    endDelim: Char,
): Lexer {
    return when (val char = code.getOrNull(pos)) {
        null -> return err("Missing $endDelim", pos, pos)
        '\n' -> Token.EOL.add(pos + 1)
        '\t', ' ', '\r' -> lexWhitespace(code, pos + 1).add()
        '#' -> lexRawString(code, pos + 1)
        '(' -> {
            val nested = Lexer(pos + 1).lexNested(code, ')')
            syncErrors(nested).apply {
                Token.Parens(nested.tokens.buildList()).add(nested.pos)
            }
        }
        '[' -> {
            val nested = Lexer(pos + 1).lexNested(code, ']')
            syncErrors(nested).apply {
                Token.Brackets(nested.tokens.buildList()).add(nested.pos)
            }
        }
        '{' -> {
            val nested = Lexer(pos + 1).lexNested(code, '}')
            syncErrors(nested).apply {
                Token.Braces(nested.tokens.buildList()).add(nested.pos)
            }
        }
        ')' -> if (endDelim == ')') {
            return this.copy(pos = pos + 1)
        } else err("Unexpected character", pos)
        ']' -> if (endDelim == ']') {
            return this.copy(pos = pos + 1)
        } else err("Unexpected character", pos)
        '}' -> if (endDelim == '}') {
            return this.copy(pos = pos + 1)
        } else err("Unexpected character", pos)
        ',' -> Token.Comma.add(pos + 1)
        ';' -> Token.Semicolon.add(pos + 1)
        '@' -> Token.At.add(pos + 1)
        '?' -> Token.QuestionMark.add(pos + 1)
        ':' -> if (code.getOrNull(pos + 1) == ':') {
            Token.DoubleColon.add(pos + 2)
        } else {
            Token.Colon.add(pos + 1)
        }
        '.' -> if (code.getOrNull(pos + 1) == '.') {
            Token.RangeTo.add(pos + 2)
        } else {
            Token.Dot.add(pos + 1)
        }
        '0' -> when (code.getOrNull(pos + 1)) {
            'b', 'B' -> lexBinaryNumber(code, pos + 2, StringBuilder()).add()
            'o', 'O' -> lexOctalNumber(code, pos + 2, StringBuilder()).add()
            'z', 'Z' -> lexDozenalNumber(code, pos + 2, StringBuilder()).add()
            'x', 'X' -> lexHexNumber(code, pos + 2, StringBuilder()).add()
            else -> lexNumber(code, pos + 1, StringBuilder("0"))
        }
        in '1'..'9' -> lexNumber(code, pos + 1, StringBuilder().append(char))
        '\'' -> lexChar(code, pos + 1)
        '\"' -> lexString(code, pos + 1, emptyList(), StringBuilder(), 0)
        '`' -> lexTickedIdent(code, pos + 1, StringBuilder())
        '/' -> when (code.getOrNull(pos + 1)) {
            '/' -> lexSingleLineComment(code, pos + 2).add()
            '*' -> lexMultiLineComment(code, pos + 2)
            else -> lexSymbol(code, pos + 1, '/')
        }
        in identStartChars -> lexNormalIdent(code, pos + 1, StringBuilder().append(char)).add()
        in symbolChars -> lexSymbol(code, pos + 1, char)
        in confusables -> {
            val confusable = confusables[char]!!
            Token.Error("Found a ${confusable.first}, which looks like a ${charNames[confusable.second]}").add(pos + 1)
        }
        else -> Token.Error("Unsupported character").add(pos + 1)
    }.lexNested(code, endDelim)
}

private tailrec fun Lexer.lexRawString(code: String, pos: StringPos, hashes: Int = 1): Lexer =
    when (code.getOrNull(pos)) {
        '#' -> lexRawString(code, pos + 1, hashes + 1)
        '"' -> lexString(code, pos, emptyList(), StringBuilder(), hashes)
        else -> addErr("Expected a double colon", this.pos + 1, pos)
    }

private fun lexSymbolChars(code: String, pos: StringPos, builder: StringBuilder): StringBuilder =
    when (val char = code.getOrNull(pos)) {
        in symbolChars -> lexSymbolChars(code, pos + 1, builder.append(char))
        else -> builder
    }

private fun Lexer.lexSymbol(code: String, pos: StringPos, first: Char): Lexer =
    when (val symbol = lexSymbolChars(code, pos, StringBuilder().append(first)).toString()) {
        "+" -> Token.Plus.add(pos + 1)
        "-" -> Token.Minus.add(pos + 1)
        "*" -> Token.Times.add(pos + 1)
        "/" -> Token.Div.add(pos + 1)
        "%" -> Token.Rem.add(pos + 1)
        "&&" -> Token.And.add(pos + 2)
        "||" -> Token.Or.add(pos + 2)
        "!" -> Token.ExclamationMark.add(pos + 1)
        "==" -> Token.Eq.add(pos + 2)
        "!=" -> Token.NotEq.add(pos + 2)
        "===" -> Token.RefEq.add(pos + 3)
        "!==" -> Token.NotRefEq.add(pos + 3)
        "=" -> Token.Assign.add(pos + 1)
        "+=" -> Token.PlusAssign.add(pos + 2)
        "-=" -> Token.MinusAssign.add(pos + 2)
        "*=" -> Token.TimesAssign.add(pos + 2)
        "/=" -> Token.DivAssign.add(pos + 2)
        "%=" -> Token.RemAssign.add(pos + 2)
        "->" -> Token.Arrow.add(pos + 1)
        ">" -> Token.Greater.add(pos + 1)
        "<" -> Token.Less.add(pos + 1)
        ">=" -> Token.GreaterOrEq.add(pos + 2)
        "<=" -> Token.GreaterOrEq.add(pos + 2)
        else -> addErr("Unknown operator", pos, pos + symbol.length)
    }

internal tailrec fun Lexer.lexNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): Lexer = when (val char = code.getOrNull(pos)) {
    in '0'..'9' ->
        lexNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexNumber(code, pos + 1, builder)
    '.' -> {
        val next = code.getOrNull(pos + 1)
        if (next in '0'..'9') lexDecimalNumber(code, pos + 2, builder.append(char).append(next))
        Token.IntLit(builder.toString().toLong()).add(pos)
    }
    else -> Token.IntLit(builder.toString().toLong()).add(pos)
}

private tailrec fun Lexer.lexDecimalNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): Lexer = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalNumber(code, pos + 1, builder.append(char))
    '_' -> lexDecimalNumber(code, pos + 1, builder)
    'e' -> when (val exponentStart = code.getOrNull(pos + 1)) {
        '+', '-' -> {
            val digit = code.getOrNull(pos + 2)
            if (digit?.isDigit() == true)
                lexDecimalExponent(code, pos + 3, builder.append(exponentStart).append(digit)).add()
            else addErr("Invalid exponent", pos + 2)
        }
        in '0'..'9' -> lexDecimalExponent(code, pos + 2, builder.append(exponentStart)).add()
        else -> addErr("Invalid exponent", pos + 1)
    }
    else -> Token.FloatLit(builder.toString().toDouble()).add(pos)
}

private tailrec fun lexDecimalExponent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> lexDecimalExponent(code, pos + 1, builder.append(char))
    '_' -> lexDecimalExponent(code, pos + 1, builder)
    else -> Token.FloatLit(builder.toString().toDouble()).till(pos)
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
    else -> Token.IntLit(builder.toString().toLong(radix = 2)).till(pos)
}

internal tailrec fun lexOctalNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'7' ->
        lexOctalNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexOctalNumber(code, pos + 1, builder)
    else -> Token.IntLit(builder.toString().toLong(radix = 8)).till(pos)
}

internal tailrec fun lexDozenalNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9', 'a', 'b', 'A', 'B' ->
        lexDozenalNumber(code, pos + 1, builder.append(char))
    '_' ->
        lexDozenalNumber(code, pos + 1, builder)
    else -> Token.IntLit(builder.toString().toLong(radix = 12)).till(pos)
}

internal tailrec fun lexHexNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> lexHexNumber(code, pos + 1, builder.append(char))
    '_' -> lexHexNumber(code, pos + 1, builder)
    else -> Token.IntLit(builder.toString().toLong(radix = 16)).till(pos)
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

internal fun Lexer.lexChar(code: String, pos: StringPos): Lexer {
    val contained: Char
    val lexerBeforeEnd: Lexer
    val lastPos: Int

    when (val char = code.getOrNull(pos)) {
        null, '\n' -> return addErr("Lone single quote", this.pos, pos)
        '\\' -> when (val res = handleEscaped(code, pos + 1, 0)) {
            is LexResult.Success -> {
                contained = res.value
                lexerBeforeEnd = this
                lastPos = res.next
            }
            is LexResult.Error -> {
                contained = ' '
                lexerBeforeEnd = err(res.error)
                lastPos = res.error.next
            }
        }
        else -> {
            contained = char
            lexerBeforeEnd = this
            lastPos = pos + 1
        }
    }

    val endChar = code.getOrNull(lastPos)
    return lexerBeforeEnd.apply {
        when {
            endChar == '\'' -> Token.CharLit(contained).add(lastPos + 1)
            endChar == null -> addErr("Unclosed char literal", lastPos)
            endChar == ' ' && contained == ' ' -> isMalformedTab(code, pos + 2)?.let {
                addErr("Malformed tab", pos, it + 1)
            } ?: addErr("Missing single quote", lastPos)
            else -> addErr("Missing single quote", lastPos)
        }
    }
}

private tailrec fun isMalformedTab(code: String, pos: StringPos): StringPos? = when (code.getOrNull(pos)) {
    null -> null
    ' ' -> isMalformedTab(code, pos)
    '\'' -> pos
    else -> null
}