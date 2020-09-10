package com.scientianova.palm.lexer

import com.scientianova.palm.errors.emptyIdent
import com.scientianova.palm.errors.invalidBacktickedIdentifier
import com.scientianova.palm.errors.unclosedIdentifier
import com.scientianova.palm.util.StringPos

internal tailrec fun lexNormalIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
): PToken = when (val char = code.getOrNull(pos)) {
    in identChars -> lexNormalIdent(code, pos + 1, builder.append(char))
    else -> matchIdentToken(builder.toString(), code, pos)
}

private fun matchIdentToken(ident: String, code: String, nextPos: StringPos) = when (ident) {
    "class" -> Token.Class.to(nextPos)
    "object" -> Token.Object.to(nextPos)
    "record" -> Token.Record.to(nextPos)
    "enum" -> Token.Enum.to(nextPos)
    "abstract" -> Token.Abstract.to(nextPos)
    "leaf" -> Token.Leaf.to(nextPos)
    "using" -> Token.Using.to(nextPos)
    "given" -> Token.Given.to(nextPos)
    "override" -> Token.Override.to(nextPos)
    "val" -> Token.Val.to(nextPos)
    "var" -> Token.Var.to(nextPos)
    "inline" -> Token.Inline.to(nextPos)
    "by" -> Token.By.to(nextPos)
    "get" -> Token.Get.to(nextPos)
    "set" -> Token.Set.to(nextPos)
    "lateinit" -> Token.Lateinit.to(nextPos)
    "noinline" -> Token.Noinline.to(nextPos)
    "crossinline" -> Token.Crossinline.to(nextPos)
    "where" -> Token.Where.to(nextPos)
    "fun" -> Token.Fun.to(nextPos)
    "when" -> Token.When.to(nextPos)
    "if" -> Token.If.to(nextPos)
    "else" -> Token.Else.to(nextPos)
    "loop" -> Token.Loop.to(nextPos)
    "while" -> Token.While.to(nextPos)
    "for" -> Token.For.to(nextPos)
    "break" -> Token.Break.to(nextPos)
    "continue" -> Token.Continue.to(nextPos)
    "return" -> Token.Return.to(nextPos)
    "throw" -> Token.Throw.to(nextPos)
    "nobreak" -> Token.Nobreak.to(nextPos)
    "guard" -> Token.Guard.to(nextPos)
    "do" -> Token.Do.to(nextPos)
    "import" -> Token.Import.to(nextPos)
    "package" -> Token.Package.to(nextPos)
    "is" -> Token.Is.to(nextPos)
    "as" -> when (code.getOrNull(nextPos)) {
        '!' -> Token.UnsafeAs.to(nextPos + 1)
        '?' -> Token.NullableAs.to(nextPos + 1)
        else -> Token.As.to(nextPos)
    }
    "null" -> Token.Null.to(nextPos)
    "this" -> Token.This.to(nextPos)
    "super" -> Token.Super.to(nextPos)
    "true" -> trueToken.to(nextPos)
    "false" -> falseToken.to(nextPos)
    "type" -> Token.Type.to(nextPos)
    "extend" -> Token.Extend.to(nextPos)
    "public" -> Token.Public.to(nextPos)
    "private" -> Token.Private.to(nextPos)
    "protected" -> Token.Protected.to(nextPos)
    "internal" -> Token.Internal.to(nextPos)
    "fallthrough" -> Token.Fallthrough.to(nextPos)
    "in" -> Token.In.to(nextPos)
    "out" -> Token.Out.to(nextPos)
    "tailrec" -> Token.Tailrec.to(nextPos)
    "suspend" -> Token.Suspend.to(nextPos)
    "with" -> Token.With.to(nextPos)
    "init" -> Token.Init.to(nextPos)
    "constructor" -> Token.Constructor.to(nextPos)
    "operator" -> Token.Operator.to(nextPos)
    else -> if (ident.all { it == '_' }) {
        Token.Wildcard.to(nextPos)
    } else {
        Token.Ident(ident).to(nextPos)
    }
}

internal tailrec fun lexTickedIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null -> unclosedIdentifier.token(pos)
    '/', '\\', '.', ';', ':', '<', '>', '[', ']' -> invalidBacktickedIdentifier.token(pos)
    '`' -> {
        val ident = builder.toString()
        when {
            ident.isBlank() -> emptyIdent.token(pos - ident.length - 1, pos + 1)
            else -> Token.Ident(ident).to(pos + 1)
        }
    }
    else -> lexTickedIdent(code, pos + 1, builder.append(char))
}

