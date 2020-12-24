package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

internal tailrec fun lexNormalIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in identChars -> lexNormalIdent(code, pos + 1, builder.append(char))
    else -> matchIdentToken(builder.toString()).till(pos)
}

private fun matchIdentToken(ident: String) = when (ident) {
    "class" -> Token.Class
    "object" -> Token.Object
    "interface" -> Token.Interface
    "val" -> Token.Val
    "var" -> Token.Var
    "fun" -> Token.Fun
    "when" -> Token.When
    "if" -> Token.If
    "else" -> Token.Else
    "break" -> Token.Break
    "return" -> Token.Return
    "throw" -> Token.Throw
    "do" -> Token.Do
    "catch" -> Token.Catch
    "defer" -> Token.Defer
    "import" -> Token.Import
    "is" -> Token.Is
    "in" -> Token.In
    "as" -> Token.As
    "null" -> Token.NullLit
    "super" -> Token.Super
    "true" -> trueToken
    "false" -> falseToken
    else -> if (ident.all('_'::equals)) {
        Token.Wildcard
    } else {
        Token.Ident(ident, false)
    }
}

internal tailrec fun Lexer.lexTickedIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): Lexer = when (val char = code.getOrNull(pos)) {
    null -> addErr("Unclosed identifier", this.pos + 1, pos)
    '/', '\\', '.', ';', ':', '<', '>', '[', ']' ->
        err("Unsupported character inside identifier", pos).lexTickedIdent(code, pos, builder)
    '`' -> {
        val ident = builder.toString()
        if (ident.isBlank()) err("Empty identifier", pos - 1, pos + 1).apply {
            Token.Ident(ident, true).add(pos + 1)
        } else {
            Token.Ident(ident, true).add(pos + 1)
        }
    }
    else -> lexTickedIdent(code, pos + 1, builder.append(char))
}