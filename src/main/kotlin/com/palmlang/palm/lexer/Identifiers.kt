package com.palmlang.palm.lexer

import com.palmlang.palm.util.StringPos

internal tailrec fun Lexer.lexNormalIdent(
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in identChars -> lexNormalIdent(pos + 1, builder.append(char))
    else -> matchIdentToken(builder.toString()).end(pos)
}

private fun matchIdentToken(ident: String) = when (ident) {
    "class" -> Token.Class
    "object" -> Token.Object
    "interface" -> Token.Interface
    "def" -> Token.Def
    "let" -> Token.Let
    "mut" -> Token.Mut
    "when" -> Token.When
    "return" -> Token.Return
    "import" -> Token.Import
    "is" -> Token.Is
    "as" -> Token.As
    "null" -> Token.NullLit
    "sumod" -> Token.SuMod
    "mod" -> Token.Mod
    "type" -> Token.Type
    "constructor" -> Token.Constructor
    "impl" -> Token.Impl
    "true" -> trueToken
    "false" -> falseToken
    "where" -> whereIdent
    "in" -> inIdent
    "out" -> outIdent
    "get" -> getIdent
    "set" -> setIdent
    else -> if (ident.all('_'::equals)) {
        Token.Wildcard
    } else {
        Token.Ident(ident, false)
    }
}

internal tailrec fun Lexer.lexTickedIdent(
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null -> createErr("Unclosed identifier", this.pos + 1, pos)
    '/', '\\', '.', ';', ':', '<', '>', '[', ']' ->
        err("Unsupported character inside identifier", pos).lexTickedIdent(pos, builder)
    '`' -> {
        val ident = builder.toString()
        if (ident.isBlank()) err("Empty identifier", pos - 1, pos + 1).run {
            Token.Ident(ident, true).end(pos + 1)
        } else {
            Token.Ident(ident, true).end(pos + 1)
        }
    }
    else -> lexTickedIdent(pos + 1, builder.append(char))
}

val fileIdent = Token.Ident("file", false)
val whereIdent = Token.Ident("where", false)
val inIdent = Token.Ident("in", false)
val outIdent = Token.Ident("out", false)
val getIdent = Token.Ident("get", false)
val setIdent = Token.Ident("set", false)
