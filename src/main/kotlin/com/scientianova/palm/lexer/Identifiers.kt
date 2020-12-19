package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

internal tailrec fun lexNormalIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in identChars -> lexNormalIdent(code, pos + 1, builder.append(char))
    else -> matchIdentToken(builder.toString(), pos)
}

private fun matchIdentToken(ident: String, nextPos: StringPos) = when (ident) {
    "class" -> Token.Class to nextPos
    "object" -> Token.Object to nextPos
    "enum" -> Token.Enum to nextPos
    "val" -> Token.Val to nextPos
    "var" -> Token.Var to nextPos
    "fun" -> Token.Fun to nextPos
    "when" -> Token.When to nextPos
    "if" -> Token.If to nextPos
    "else" -> Token.Else to nextPos
    "break" -> Token.Break to nextPos
    "return" -> Token.Return to nextPos
    "throw" -> Token.Throw to nextPos
    "do" -> Token.Do to nextPos
    "catch" -> Token.Catch to nextPos
    "defer" -> Token.Defer to nextPos
    "import" -> Token.Import to nextPos
    "is" -> Token.Is to nextPos
    "as" -> Token.As to nextPos
    "null" -> Token.Null to nextPos
    "super" -> Token.Super to nextPos
    "true" -> trueToken to nextPos
    "false" -> falseToken to nextPos
    else -> if (ident.all { it == '_' }) {
        Token.Wildcard to nextPos
    } else {
        Token.Ident(ident, false) to nextPos
    }
}

internal tailrec fun lexTickedIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null -> Token.Error("Unclosed identifier") to pos + 1
    '/', '\\', '.', ';', ':', '<', '>', '[', ']' -> Token.Error("Unsupported character inside identifier") to pos + 1
    '`' -> {
        val ident = builder.toString()
        if (ident.isBlank()) {
            Token.Error("Empty identifier") to pos + 1
        } else {
            Token.Ident(ident, true) to pos + 1
        }
    }
    else -> lexTickedIdent(code, pos + 1, builder.append(char))
}