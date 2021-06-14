package com.palmlang.palm.lexer

import com.palmlang.palm.util.StringPos

internal tailrec fun Lexer.lexString(
    pos: StringPos,
    parts: List<StringPartL>,
    builder: StringBuilder,
    hashes: Int
): PToken = when (val char = code.getOrNull(pos)) {
    null -> createErr("Missing double quote", this.pos + 1, pos)
    '"' -> if (hashNumEq(code, pos + 1, hashes)) {
        Token.StrLit(parts + StringPartL.String(builder.toString())).end(pos + hashes + 1)
    } else lexString( pos + 1, parts, builder.append('"'), hashes)
    '$' -> {
        val interPos = pos + 1
        when (code.getOrNull(interPos)) {
            '{' -> {
                val nested = nestedLexerAt(interPos + 1).lexNested( '}')
                lexString(
                     nested.pos,
                    parts + StringPartL.Expr(interPos, nested.tokens),
                    builder.clear(), hashes
                )
            }
            '`' -> {
                val nested = lexTickedIdent(interPos + 1, StringBuilder())
                lexString(
                     nested.next,
                    parts + StringPartL.Expr(interPos, listOf(nested)),
                    builder.clear(), hashes
                )
            }
            in identStartChars -> {
                val nested = lexNormalIdent(interPos + 1, StringBuilder())
                lexString(
                     nested.next,
                    parts + StringPartL.Expr(interPos, listOf(nested)),
                    builder.clear(), hashes
                )
            }
            else -> lexString( pos + 1, parts, builder.append('$'), hashes)
        }
    }
    '\\' -> when (val res = handleEscaped( pos + 1, hashes)) {
        is LexResult.Success -> lexString( res.next, parts, builder.append(res.value), hashes)
        is LexResult.Error -> err(res.error).lexString(res.error.next, parts, builder, hashes)
    }
    else -> lexString( pos + 1, parts, builder.append(char), hashes)
}