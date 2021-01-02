package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

internal tailrec fun Lexer.lexString(
    code: String,
    pos: StringPos,
    parts: List<StringPartL>,
    builder: StringBuilder,
    hashes: Int
): Lexer = when (val char = code.getOrNull(pos)) {
    null -> addErr("Missing double quote", this.pos + 1, pos)
    '"' -> if (hashNumEq(code, pos + 1, hashes)) {
        Token.StrLit(parts + StringPartL.String(builder.toString())).add(pos + hashes + 1)
    } else lexString(code, pos + 1, parts, builder.append('"'), hashes)
    '$' -> {
        val interPos = pos + 1
        when (code.getOrNull(interPos)) {
            '{' -> {
                val nested = Lexer(interPos + 1).lexNested(code, '}')
                lexString(
                    code, nested.pos,
                    parts + StringPartL.Expr(interPos, nested.tokens),
                    builder.clear(), hashes
                )
            }
            '`' -> {
                val nested = Lexer(interPos + 1).lexTickedIdent(code, interPos + 1, StringBuilder()).endHere()
                lexString(
                    code, nested.pos,
                    parts + StringPartL.Expr(interPos, nested.tokens),
                    builder.clear(), hashes
                )
            }
            in identStartChars -> {
                val nested = Lexer(interPos + 1).lexNormalIdent(code, interPos + 1, StringBuilder()).endHere()
                lexString(
                    code, nested.pos,
                    parts + StringPartL.Expr(interPos, nested.tokens),
                    builder.clear(), hashes
                )
            }
            else -> lexString(code, pos + 1, parts, builder.append('$'), hashes)
        }
    }
    '\\' -> when (val res = handleEscaped(code, pos + 1, hashes)) {
        is LexResult.Success -> lexString(code, res.next, parts, builder.append(res.value), hashes)
        is LexResult.Error -> err(res.error).lexString(code, res.error.next, parts, builder, hashes)
    }
    else -> lexString(code, pos + 1, parts, builder.append(char), hashes)
}