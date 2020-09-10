package com.scientianova.palm.lexer

import com.scientianova.palm.errors.missingDoubleQuote
import com.scientianova.palm.errors.unclosedMultilineString
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map
import com.scientianova.palm.util.mapFirst

internal tailrec fun lexSingleLineString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null, '\n' -> missingDoubleQuote.token(pos)
    '"' -> Token.Str(parts + StringPart.Regular(builder.toString())).to(pos + 1)
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' ->
                TODO()
            '`' ->
                lexTickedIdent(code, interPos + 1, StringBuilder()).mapFirst { TODO() }
            in identStartChars ->
                lexNormalIdent(code, interPos + 1, StringBuilder().append(interChar)).mapFirst { TODO() }
            else ->
                lexSingleLineString(code, pos + 1, parts, builder.append('$'))
        }
    }
    '\\' -> when (val res = handleEscaped(code, pos + 1)) {
        is LexResult.Success ->
            lexSingleLineString(code, res.next, parts, builder.append(res.value))
        is LexResult.Error -> res.error.token(res.start, res.next)
    }
    else -> lexSingleLineString(code, pos + 1, parts, builder.append(char))
}

internal tailrec fun lexMultiLineString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null -> unclosedMultilineString.token(pos)
    '"' -> if (code.startsWith("\"\"", pos + 1)) {
        Token.Str(parts + StringPart.Regular(builder.toString())).to(pos + 1)
    } else lexMultiLineString(code, pos + 1, parts, builder.append('"'))
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' ->
                TODO()
            '`' ->
                lexTickedIdent(code, interPos + 1, StringBuilder()).mapFirst { TODO() }
            in identStartChars ->
                lexNormalIdent(code, interPos + 1, StringBuilder().append(interChar)).mapFirst { TODO() }
            else ->
                lexMultiLineString(code, pos + 1, parts, builder.append('$'))
        }
    }
    else -> lexMultiLineString(code, pos + 1, parts, builder.append(char))
}