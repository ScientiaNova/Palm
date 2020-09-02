package com.scientianova.palm.parser

fun PExpr.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("typeToCodeString")
fun PType.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("patternToCodeString")
fun PPattern.toCodeString(indent: Int) = value.toCodeString(indent)

fun Pattern.toCodeString(indent: Int): String = when (this) {
    is WildcardPattern -> "_"
    is ExprPattern -> expr.toCodeString(indent)
    is DecPattern -> "${if (mutable) "var" else "val"} $name"
    is TypePattern -> "is ${type.toCodeString(indent)}"
    is EnumPattern -> ".$name(${params.joinToString { it.toCodeString(indent) }})"
}

fun Type.toCodeString(indent: Int): String = when (this) {
    is NamedType -> "${path.joinToString(".") { it.value }}[${generics.joinToString { it.toCodeString(indent) }}]"
    is FunctionType -> "(${params.joinToString { it.toCodeString(indent) }}) -> ${returnType.toCodeString(indent)}"
}

fun Condition.toCodeString(indent: Int): String = when (this) {
    is ExprCondition -> expr.toCodeString(indent)
    is DecCondition -> "${pattern.toCodeString(indent)} = ${expr.toCodeString(indent)}"
}

fun ScopeStatement.toCodeString(indent: Int): String = when (this) {
    is ExprStatement -> expr.toCodeString(indent)
    is VarDecStatement -> "${if (mutable) "val" else "var"} $name" +
            (type?.let { ": ${it.toCodeString(indent)}" } ?: "") +
            (expr?.let { " = ${it.toCodeString(indent)}" } ?: "")
}

fun ExprScope.toCodeString(indent: Int): String = """
{
${indent(indent + 1)}${statements.joinToString("\n" + indent(indent + 1)) { it.toCodeString(indent + 1) }}
${indent(indent)}}
""".trimIndent()

private fun indent(count: Int) = "    ".repeat(count)

fun Expression.toCodeString(indent: Int): String = when (this) {
    is IdentExpr -> name
    is BoolExpr -> value.toString()
    is NullExpr -> "null"
    is ThisExpr -> "this"
    is ByteExpr -> value.toString()
    is ShortExpr -> value.toString()
    is IntExpr -> value.toString()
    is LongExpr -> value.toString()
    is FloatExpr -> value.toString()
    is DoubleExpr -> value.toString()
    is CharExpr -> "'$value'"
    is StringExpr -> "\"$string\""
    is IfExpr -> "if ${cond.joinToString { it.toCodeString(indent) }} ${ifTrue.toCodeString(indent)}" +
            (ifFalse?.let { " else ${it.toCodeString(indent)}" } ?: "")
    is ScopeExpr -> scope.toCodeString(indent)
    is WhenExpr -> """
when ${comparing?.let { it.value.toCodeString(indent) + " " } ?: ""}{
${indent(indent + 1)}${
        branches.joinToString("\n" + indent(indent + 1)) {
            it.first.value.toCodeString(indent + 1) + " -> " + it.second.toCodeString(indent + 1)
        }
    }
${indent(indent)}}
""".trimIndent()
    is ListExpr -> "[${components.joinToString { it.toCodeString(indent) }}]"
    is ForExpr -> "for $name in ${iterable.toCodeString(indent)} ${body.toCodeString(indent)}"
    is CallExpr -> expr.toCodeString(indent) + args.toCodeString(indent)
    is OpRefExpr -> symbol
    is LambdaExpr -> if (params.isEmpty()) {
        scope.toCodeString(indent)
    } else {
        val paramsString = params.joinToString {
            it.first.value + (it.second?.let { type -> ": ${type.toCodeString(indent)}" } ?: "")
        }
        """
{ $paramsString ->
${indent(indent + 1)}${scope.statements.joinToString("\n" + indent(indent + 1)) { it.toCodeString(indent + 1) }}
${indent(indent)}}
        """.trimIndent()
    }
}

fun Arg.toCodeString(indent: Int) = when (this) {
    is Arg.Free -> value.toCodeString(indent)
    is Arg.Named -> "$name = ${value.toCodeString(indent)}"
}

fun CallArgs.toCodeString(indent: Int): String {
    val end = last?.let {
        " " + it.toCodeString(indent)
    } ?: ""
    return if (args.isEmpty() && end.isNotBlank()) end
    else "(${args.joinToString { it.toCodeString(indent) }})$end"
}