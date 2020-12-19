package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

internal tailrec fun lexString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder,
    hashes: Int
): PToken = when (val char = code.getOrNull(pos)) {
    null -> Token.Error("Missing colon") to pos
    '"' -> if (hashNumEq(code, pos + 1, hashes)) {
        Token.Str(parts + StringPart.String(builder.toString())) to pos + hashes + 1
    } else lexString(code, pos + 1, parts, builder.append('"'), hashes)
    '$' -> {
        val interPos = pos + 1
        when (code.getOrNull(interPos)) {
            '{' -> {
                val nested = lexNested(code, '}', interPos + 1)
                lexString(
                    code, nested.second,
                    parts + StringPart.Expr(nested),
                    builder.clear(), hashes
                )
            }
            '`' -> {
                val token = lexTickedIdent(code, interPos + 1, StringBuilder())
                lexString(
                    code, token.second,
                    parts + StringPart.Expr(token),
                    builder.clear(), hashes
                )
            }
            in identStartChars -> {
                val token = lexNormalIdent(code, interPos + 1, StringBuilder())
                lexString(
                    code, token.second,
                    parts + StringPart.Expr(token),
                    builder.clear(), hashes
                )
            }
            else -> lexString(code, pos + 1, parts, builder.append('$'), hashes)
        }
    }
    '\\' -> when (val res = handleEscaped(code, pos + 1, hashes)) {
        is LexResult.Success -> lexString(code, res.next, parts, builder.append(res.value), hashes)
        is LexResult.Error -> Token.Error(res.error) to res.next + 1
    }
    else -> lexString(code, pos + 1, parts, builder.append(char), hashes)
}