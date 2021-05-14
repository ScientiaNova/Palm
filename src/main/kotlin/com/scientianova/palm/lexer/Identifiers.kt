package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

internal tailrec fun Lexer.lexNormalIdent(
    pos: StringPos,
    builder: StringBuilder,
): Lexer = when (val char = code.getOrNull(pos)) {
    in identChars -> lexNormalIdent(pos + 1, builder.append(char))
    else -> matchIdentToken(builder.toString()).add(pos)
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
    "mod" -> Token.Mod
    "crate" -> Token.Crate
    "true" -> trueToken
    "false" -> falseToken
    "where" -> whereIdent
    "out" -> outIdent
    "init" -> initIdent
    "type" -> typeIdent
    "file" -> fileIdent
    "get" -> getIdent
    "set" -> setIdent
    "show" -> showIdent
    "hide" -> hideIdent
    else -> if (ident.all('_'::equals)) {
        Token.Wildcard
    } else {
        Token.Ident(ident, false)
    }
}

internal tailrec fun Lexer.lexTickedIdent(
    pos: StringPos,
    builder: StringBuilder
): Lexer = when (val char = code.getOrNull(pos)) {
    null -> addErr("Unclosed identifier", this.pos + 1, pos)
    '/', '\\', '.', ';', ':', '<', '>', '[', ']' ->
        err("Unsupported character inside identifier", pos).lexTickedIdent(pos, builder)
    '`' -> {
        val ident = builder.toString()
        if (ident.isBlank()) err("Empty identifier", pos - 1, pos + 1).run {
            Token.Ident(ident, true).add(pos + 1)
        } else {
            Token.Ident(ident, true).add(pos + 1)
        }
    }
    else -> lexTickedIdent(pos + 1, builder.append(char))
}

val whereIdent = Token.Ident("where", false)
val outIdent = Token.Ident("out", false)
val initIdent = Token.Ident("init", false)
val typeIdent = Token.Ident("type", false)
val implIdent = Token.Ident("impl", false)
val fileIdent = Token.Ident("file", false)
val getIdent = Token.Ident("get", false)
val setIdent = Token.Ident("set", false)
val showIdent = Token.Ident("show", false)
val hideIdent = Token.Ident("show", false)