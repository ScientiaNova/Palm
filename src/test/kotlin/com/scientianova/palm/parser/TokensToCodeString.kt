package com.scientianova.palm.parser

import com.scientianova.palm.lexer.StringPartL
import com.scientianova.palm.lexer.Token

fun Token.toCodeString(indent: Int): String = when (this) {
    is Token.Braces -> "{\n" + indent(indent + 1) +
                tokens.joinToString("\n" + indent(indent + 1)) { it.value.toCodeString(indent + 1) } +
                "\n${indent(indent)}}"
    is Token.Brackets -> "[\n" + indent(indent + 1) +
            tokens.joinToString("\n" + indent(indent + 1)) { it.value.toCodeString(indent + 1) } +
            "\n${indent(indent)}]"
    is Token.Parens -> "(\n" + indent(indent + 1) +
            tokens.joinToString("\n" + indent(indent + 1)) { it.value.toCodeString(indent + 1) } +
            "\n${indent(indent)})"
    Token.Dot -> "."
    Token.RangeTo -> ".."
    Token.Colon -> ":"
    Token.DoubleColon -> "::"
    Token.Semicolon -> ";"
    Token.And -> "&&"
    Token.Or -> "||"
    Token.Less -> "<"
    Token.Greater -> ">"
    Token.LessOrEq -> "<="
    Token.GreaterOrEq -> ">="
    Token.Plus -> "+"
    Token.Minus -> "-"
    Token.Times -> "*"
    Token.Div -> "/"
    Token.Rem -> "%"
    Token.Eq -> "=="
    Token.NotEq -> "!="
    Token.RefEq -> "=="
    Token.NotRefEq -> "!=="
    Token.Assign -> "="
    Token.PlusAssign -> "+="
    Token.MinusAssign -> "-="
    Token.TimesAssign -> "*="
    Token.DivAssign -> "/="
    Token.RemAssign -> "%="
    Token.QuestionMark -> "?"
    Token.Arrow -> "->"
    Token.Spread -> "*"
    Token.Wildcard -> "_"
    Token.Comma -> ","
    Token.At -> "@"
    Token.Fun -> "fun"
    Token.Val -> "val"
    Token.Var -> "var"
    Token.Object -> "object"
    Token.Super -> "super"
    Token.NullLit -> "null"
    Token.When -> "when"
    Token.If -> "if"
    Token.Else -> "else"
    Token.Do -> "do"
    Token.Break -> "break"
    Token.Return -> "return"
    Token.As -> "as"
    Token.EOL -> "EOL"
    is Token.Ident -> if (backticked) "`$name`" else name
    is Token.BoolLit -> value.toString()
    is Token.CharLit -> "'$value'"
    is Token.IntLit -> value.toString()
    is Token.FloatLit -> value.toString()
    is Token.StrLit -> "\"\":\n" + indent(indent + 1) +
            parts.joinToString("\n" + indent(indent + 1)) { it.toCodeString(indent + 1) } +
            "\n${indent(indent)}"
    Token.Is -> "is"
    Token.In -> "in"
    Token.ExclamationMark -> "!"
    Token.Class -> "class"
    Token.Interface -> "interface"
    Token.Catch -> "catch"
    Token.Defer -> "defer"
    Token.Throw -> "throw"
    Token.Import -> "import"
    Token.Whitespace -> "*Whitespace*"
    Token.Comment -> "// Comment"
    Token.End -> "*End*"
    is Token.Error -> "!!!error!!!"
}

fun StringPartL.toCodeString(indent: Int) = when (this) {
    is StringPartL.String -> "\"$string\""
    is StringPartL.Expr -> "\${\n" + indent(indent + 1) +
            tokens.joinToString("\n" + indent(indent + 1)) { it.value.toCodeString(indent + 1) } +
            "\n${indent(indent)}}"
}