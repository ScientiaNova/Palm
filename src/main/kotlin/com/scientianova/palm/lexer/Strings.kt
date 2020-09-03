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
    null, '\n' -> missingDoubleQuote.errAt(pos)
    '"' -> Token.Str(parts + StringPart.Regular(builder.toString())) succTo pos + 1
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' -> TODO()
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
    null -> unclosedMultilineString.errAt(pos)
    '"' -> if (code.startsWith("\"\"", pos + 1)) {
        Token.Str(parts + StringPart.Regular(builder.toString())) succTo pos + 1
    } else lexMultiLineString(code, pos + 1, parts, builder.append('"'))
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' -> TODO()
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