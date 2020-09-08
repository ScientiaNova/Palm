package com.scientianova.palm.lexer

import com.scientianova.palm.errors.emptyIdent
import com.scientianova.palm.errors.invalidBacktickedIdentifier
import com.scientianova.palm.errors.unclosedIdentifier
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

internal tailrec fun lexNormalIdent(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in identChars -> lexNormalIdent(code, start, pos + 1, builder.append(char))
    else -> matchIdentToken(builder.toString(), code, start, pos)
}

private fun matchIdentToken(ident: String, code: String, start: StringPos, nextPos: StringPos) = when (ident) {
    "class" -> Token.Class.at(start, nextPos)
    "object" -> Token.Object.at(start, nextPos)
    "record" -> Token.Record.at(start, nextPos)
    "enum" -> Token.Enum.at(start, nextPos)
    "abstract" -> Token.Abstract.at(start, nextPos)
    "leaf" -> Token.Leaf.at(start, nextPos)
    "using" -> Token.Using.at(start, nextPos)
    "given" -> Token.Given.at(start, nextPos)
    "override" -> Token.Override.at(start, nextPos)
    "val" -> Token.Val.at(start, nextPos)
    "var" -> Token.Var.at(start, nextPos)
    "inline" -> Token.Inline.at(start, nextPos)
    "by" -> Token.By.at(start, nextPos)
    "get" -> Token.Get.at(start, nextPos)
    "set" -> Token.Set.at(start, nextPos)
    "lateinit" -> Token.Lateinit.at(start, nextPos)
    "noinline" -> Token.Noinline.at(start, nextPos)
    "crossinline" -> Token.Crossinline.at(start, nextPos)
    "where" -> Token.Where.at(start, nextPos)
    "fun" -> Token.Fun.at(start, nextPos)
    "when" -> Token.When.at(start, nextPos)
    "if" -> Token.If.at(start, nextPos)
    "else" -> Token.Else.at(start, nextPos)
    "loop" -> Token.Loop.at(start, nextPos)
    "while" -> Token.While.at(start, nextPos)
    "for" -> Token.For.at(start, nextPos)
    "break" -> Token.Break.at(start, nextPos)
    "continue" -> Token.Continue.at(start, nextPos)
    "return" -> Token.Return.at(start, nextPos)
    "throw" -> Token.Throw.at(start, nextPos)
    "nobreak" -> Token.Nobreak.at(start, nextPos)
    "guard" -> Token.Guard.at(start, nextPos)
    "do" -> Token.Do.at(start, nextPos)
    "import" -> Token.Import.at(start, nextPos)
    "package" -> Token.Package.at(start, nextPos)
    "is" -> Token.Is.at(start, nextPos)
    "as" -> when (code.getOrNull(nextPos)) {
        '!' -> Token.UnsafeAs.at(start, nextPos + 1)
        '?' -> Token.NullableAs.at(start, nextPos + 1)
        else -> Token.As.at(start, nextPos)
    }
    "null" -> Token.Null.at(start, nextPos)
    "this" -> Token.This.at(start, nextPos)
    "super" -> Token.Super.at(start, nextPos)
    "true" -> trueToken.at(start, nextPos)
    "false" -> falseToken.at(start, nextPos)
    "type" -> Token.Type.at(start, nextPos)
    "extend" -> Token.Extend.at(start, nextPos)
    "public" -> Token.Public.at(start, nextPos)
    "private" -> Token.Private.at(start, nextPos)
    "protected" -> Token.Protected.at(start, nextPos)
    "internal" -> Token.Internal.at(start, nextPos)
    "fallthrough" -> Token.Fallthrough.at(start, nextPos)
    "in" -> Token.In.at(start, nextPos)
    "out" -> Token.Out.at(start, nextPos)
    "tailrec" -> Token.Tailrec.at(start, nextPos)
    "suspend" -> Token.Suspend.at(start, nextPos)
    "with" -> Token.With.at(start, nextPos)
    "init" -> Token.Init.at(start, nextPos)
    "constructor" -> Token.Constructor.at(start, nextPos)
    "operator" -> Token.Operator.at(start, nextPos)
    else -> if (ident.all { it == '_' }) {
        Token.Wildcard.at(start, nextPos)
    } else {
        Token.Ident(ident).at(start, nextPos)
    }
}

internal tailrec fun lexTickedIdent(
    code: String,
    start: StringPos,
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null -> unclosedIdentifier.token(pos)
    '/', '\\', '.', ';', ':', '<', '>', '[', ']' -> invalidBacktickedIdentifier.token(pos)
    '`' -> {
        val ident = builder.toString()
        val nextPos = pos + 1
        when {
            ident.isEmpty() -> emptyIdent.token(start, pos)
            else -> Token.Ident(ident).at(start, nextPos)
        }
    }
    else -> lexTickedIdent(code, start, pos + 1, builder.append(char))
}