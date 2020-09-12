package com.scientianova.palm.parser

import com.scientianova.palm.lexer.StringPart
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.data.types.Enum

fun <T> T?.mapTo(fn: (T) -> String) = if (this == null) "" else fn(this)

fun PExpr.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("typeToCodeString")
fun PType.toCodeString() = value.toCodeString()

@JvmName("patternToCodeString")
fun PPattern.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("decPatternToCodeString")
fun PDecPattern.toCodeString() = value.toCodeString()

@JvmName("typeArgToCodeString")
fun PTypeArg.toCodeString() = value.toCodeString()

fun Pattern.toCodeString(indent: Int): String = when (this) {
    is Pattern.Wildcard -> "_"
    is Pattern.Expr -> expr.toCodeString(indent)
    is Pattern.Dec -> "${if (mutable) "var" else "val"} $name"
    is Pattern.Type -> "is ${type.toCodeString()}"
    is Pattern.EnumTuple -> ".$name(${params.joinToString { it.toCodeString(indent) }})"
    is Pattern.Enum -> ".$name(${params.joinToString { "${it.first.value}: ${it.second.value.toCodeString(indent)}" }})"
    is Pattern.Tuple -> "(${elements.joinToString { it.toCodeString(indent) }})"
    is Pattern.Record -> "{${pairs.joinToString { "${it.first.value}: ${it.second.value.toCodeString(indent)}" }}}"
}

fun DecPattern.toCodeString(): String = when (this) {
    is DecPattern.Wildcard -> "_"
    is DecPattern.Name -> name
    is DecPattern.Tuple -> "(${elements.joinToString { it.toCodeString() }})"
    is DecPattern.Record -> "{${pairs.joinToString { "${it.first.value}: ${it.second.value.toCodeString()}" }}}"
}

fun Type.toCodeString(): String = when (this) {
    is Type.Named -> "${path.joinToString(".") { it.value }}[${generics.joinToString { it.toCodeString() }}]"
    is Type.Function -> "(${params.joinToString { it.toCodeString() }}) -> ${returnType.toCodeString()}"
    is Type.Nullable -> type.toCodeString() + '?'
    is Type.Intersection -> types.joinToString(" + ") { it.toCodeString() }
}

private fun typeAnn(type: PType?) = type.mapTo { ": ${it.toCodeString()}" }

fun VarianceMod.toCodeString() = when (this) {
    VarianceMod.In -> "in "
    VarianceMod.Out -> "out "
    VarianceMod.None -> ""
}

fun TypeArg.toCodeString() = when (this) {
    is TypeArg.Normal -> variance.toCodeString() + type.toCodeString()
    is TypeArg.Wildcard -> "_"
}

fun FunTypeArg.toCodeString() = (if (using) "using " else "") + type.toCodeString()

fun TypeParam.toCodeString() = name.value

@JvmName("typeParamsToCodeString")
fun List<TypeParam>.toCodeString() = if (isEmpty()) "" else joinToString { it.toCodeString() }

@JvmName("pathToCodeString")
fun Path.toCodeString() = joinToString(".") { it.value }

fun Import.toCodeString() = "import " + when (this) {
    is Import.Regular -> path + alias.mapTo { " as $it" }
    is Import.Package -> "${path.toCodeString()}._"
}

fun Alias.toCodeString() = "type $name$params = ${actual.toCodeString()}"

fun Record.toCodeString(indent: Int) = "record " + when (this) {
    is Record.Tuple -> "$name${typeParams.toCodeString()}(${components.joinToString { it.toCodeString() }})"
    is Record.Normal -> "$name${typeParams.toCodeString()} " +
            scopeCodeString(components, indent, ",") { "${it.first}: ${it.second.toCodeString()}" }
    is Record.Single -> name.value
}

fun EnumCase.toCodeString(indent: Int) = when (this) {
    is EnumCase.Tuple -> "$name(${components.joinToString { it.toCodeString() }})"
    is EnumCase.Normal -> name.value +
            scopeCodeString(components, indent, ",") { "${it.first}: ${it.second.toCodeString()}" }
    is EnumCase.Single -> name.value
}

fun Enum.toCodeString(indent: Int) = "enum $name${typeParams.toCodeString()} " +
        scopeCodeString(cases, indent, ",") { it.value.toCodeString(indent) }

fun Condition.toCodeString(indent: Int): String = when (this) {
    is Condition.Expr -> expr.toCodeString(indent)
    is Condition.Pattern -> "${pattern.toCodeString(indent)} = ${expr.toCodeString(indent)}"
}

fun ScopeStatement.toCodeString(indent: Int): String = when (this) {
    is ExprStatement -> expr.toCodeString(indent)
    is DecStatement -> "${if (mutable) "val" else "var"} ${pattern.toCodeString()}" + typeAnn(type) +
            expr.mapTo { " = ${it.toCodeString(indent)}" }
    is UsingStatement -> "using ${expr.toCodeString(indent)}"
    is AssignStatement -> "${left.toCodeString(indent)} ${type.toCodeString()} ${right.toCodeString(indent)}"
    is GuardStatement -> "guard ${cond.toCodeString(indent)} else ${body.toCodeString(indent)}"
}

fun ClassLevelPrivacy.toCodeString() = when (this) {
    ClassLevelPrivacy.Public -> "public"
    ClassLevelPrivacy.Private -> "private"
    ClassLevelPrivacy.Protected -> "protected"
}

fun TopLevelPrivacy.toCodeString() = when (this) {
    TopLevelPrivacy.Public -> "public"
    TopLevelPrivacy.Private -> "private"
    TopLevelPrivacy.Internal -> "internal"
}

fun Function.toCodeString(indent: Int) = "fun " + (if (typeParams.isEmpty()) "" else " ") + typeParams.toCodeString() +
        "$name(${params.toCodeString(indent)})${typeAnn(type)} " +
        expr.mapTo { (if (it.value is Expr.Scope) "" else "= ") + it.value.toCodeString(indent) }

fun <P> Getter<P>.toCodeString(indent: Int, pFn: (P) -> String): String {
    val privacyS = pFn(privacy)
    return privacyS + (if (privacyS.isEmpty()) "" else " ") + "get()${typeAnn(type)} " +
            if (expr.value is Expr.Scope) "" else "= " + expr.toCodeString(indent)
}

fun <P> Setter<P>.toCodeString(indent: Int, pFn: (P) -> String): String {
    val privacyS = pFn(privacy)
    return privacyS + (if (privacyS.isEmpty()) "" else " ") + "set($paramName${typeAnn(paramType)})${typeAnn(type)} " +
            if (expr.value is Expr.Scope) "" else "= " + expr.toCodeString(indent)
}

fun <P> Property<P>.toCodeString(indent: Int, pFn: (P) -> String) = when (this) {
    is Property.Normal -> (if (mutable) "val " else "var ") + typeParams.toCodeString() +
            (if (typeParams.isEmpty()) "" else " ") + typeParams.toCodeString() +
            name + typeAnn(type) + expr.mapTo { " = ${it.toCodeString(indent)}" } +
            getter.mapTo { "\n${indent(indent + 1)}${it.toCodeString(indent, pFn)}" } +
            setter.mapTo { "\n${indent(indent + 1)}${it.toCodeString(indent, pFn)}" }
    is Property.Delegated -> (if (mutable) "val " else "var ") + typeParams.toCodeString() +
            (if (typeParams.isEmpty()) "" else " ") + typeParams.toCodeString() +
            name + typeAnn(type) + " by " + delegate.toCodeString(indent)
}

fun ClassImplementation.toCodeString() = when (this) {
    ClassImplementation.Abstract -> "abstract "
    ClassImplementation.Full -> ""
    ClassImplementation.Leaf -> "leaf "
}

fun SuperClass.toCodeString(indent: Int) = type.toCodeString() + args.toCodeString(indent) +
        (if (mixins.isEmpty()) "" else "with {${mixins.joinToString { it.toCodeString() }}}")

fun Class.toCodeString(indent: Int): String {
    val implementsNothing = implements.isEmpty()
    return privacy.toCodeString() + "" + implementation.toCodeString() + "class " + name.value +
            typeParams.joinToString { it.value.toCodeString() } +
            primaryConstructor.mapTo { "(${it.toCodeString(indent)})" } + (if (superClass == null && implementsNothing) "" else " : ") +
            superClass.mapTo { it.toCodeString(indent) + (if (implementsNothing) ", " else "") } +
            implements.joinToString { it.toCodeString() } + " "
}


fun ClassTypeParam.toCodeString() = variance.toCodeString() + type.toCodeString()

fun ParamInfo.toCodeString() = StringBuilder().apply {
    if (using) append(" using")
    if (given) append(" given")
    when (inlineHandling) {
        InlineHandling.NoInline -> append(" noinline")
        InlineHandling.CrossInline -> append(" crossinline")
        InlineHandling.None -> {
        }
    }
}.toString()

fun FunParam.toCodeString(indent: Int) = info.toCodeString() +
        "${pattern.toCodeString()}: ${type.toCodeString()}" + default.mapTo { " = ${it.toCodeString(indent)}" }

@JvmName("funParamsToCodeString")
fun List<FunParam>.toCodeString(indent: Int) = joinToString { it.toCodeString(indent) }

fun AssignmentType.toCodeString() = when (this) {
    AssignmentType.Normal -> "="
    AssignmentType.Div -> "/="
    AssignmentType.Minus -> "-="
    AssignmentType.Plus -> "+="
    AssignmentType.Rem -> "%="
    AssignmentType.Times -> "*="
}

private inline fun <T> scopeCodeString(
    list: List<T>,
    indent: Int,
    separator: String = "",
    crossinline fn: (T) -> String
) = """
{
${indent(indent + 1)}${list.joinToString("$separator\n" + indent(indent + 1)) { fn(it) }}
${indent(indent)}}
""".trimIndent()

@JvmName("scopeToCodeString")
fun ExprScope.toCodeString(indent: Int) = scopeCodeString(this, indent) { it.toCodeString(indent + 1) }

private fun indent(count: Int) = "    ".repeat(count)

fun List<Condition>.toCodeString(indent: Int) = joinToString { it.toCodeString(indent) }

fun StringPart.toCodeString(indent: Int) = when (this) {
    is StringPart.String -> string
    is StringPart.Expr -> "$${token.toCodeString(indent)}"
}

fun Expr.toCodeString(indent: Int): String = when (this) {
    is Expr.Ident -> name
    is Expr.Bool -> value.toString()
    is Expr.Null -> "null"
    is Expr.This -> "this" + label.mapTo { "@$it" }
    is Expr.Super -> "super" + label.mapTo { "@$it" }
    is Expr.Byte -> value.toString()
    is Expr.Short -> value.toString()
    is Expr.Int -> value.toString()
    is Expr.Long -> value.toString()
    is Expr.Float -> value.toString()
    is Expr.Double -> value.toString()
    is Expr.Char -> "'$value'"
    is Expr.Str -> "\"${parts.joinToString("") { it.toCodeString(indent) }}\""
    is Expr.If -> "if ${cond.toCodeString(indent)} ${ifTrue.toCodeString(indent)}" +
            ifFalse.mapTo { " else ${it.toCodeString(indent)}" }
    is Expr.Scope -> scope.toCodeString(indent)
    is Expr.When -> """
when ${comparing.mapTo { it.value.toCodeString(indent) + " " }}{
${indent(indent + 1)}${
        branches.joinToString("\n" + indent(indent + 1)) {
            it.first.value.toCodeString(indent + 1) + " -> " + it.second.toCodeString(indent + 1)
        }
    }
${indent(indent)}}
""".trimIndent()
    is Expr.Lis -> "[${elements.joinToString { it.toCodeString(indent) }}]"
    is Expr.Map -> "[${
        if (elements.isEmpty()) ":" else elements.joinToString {
            "${it.first}: ${it.second.toCodeString(indent)}"
        }
    }]"
    is Expr.For -> "for $dec in ${iterable.toCodeString(indent)} ${body.toCodeString(indent)}" +
            noBreak.mapTo { " nobreak ${it.toCodeString(indent)}" }
    is Expr.While -> "if ${cond.toCodeString(indent)} ${body.toCodeString(indent)}" +
            noBreak.mapTo { " nobreak ${it.toCodeString(indent)}" }
    is Expr.Loop -> "loop ${body.toCodeString(indent)}"
    is Expr.Continue -> "continue" + label.mapTo { "@$it" }
    is Expr.Break -> "break" + label.mapTo { "@$it" } + expr.mapTo { " ${it.toCodeString(indent)}" }
    is Expr.Return -> "return" + label.mapTo { "@$it" } + expr.mapTo { " ${it.toCodeString(indent)}" }
    is Expr.Call -> expr.toCodeString(indent) + args.toCodeString(indent)
    is Expr.Lambda -> if (params.isEmpty()) {
        scope.toCodeString(indent)
    } else {
        val paramsString = params.joinToString { it.first.toCodeString() + typeAnn(it.second) }
        """
{ $paramsString ->
${indent(indent + 1)}${scope.joinToString("\n" + indent(indent + 1)) { it.toCodeString(indent + 1) }}
${indent(indent)}}
        """.trimIndent()
    }
    is Expr.Tuple -> "(${elements.joinToString { it.toCodeString(indent) }})"
    is Expr.TypeCheck -> "${expr.toCodeString(indent)} is ${type.toCodeString()}"
    is Expr.SafeCast -> "${expr.toCodeString(indent)} as ${type.toCodeString()}"
    is Expr.NullableCast -> "${expr.toCodeString(indent)} as? ${type.toCodeString()}"
    is Expr.UnsafeCast -> "${expr.toCodeString(indent)} as! ${type.toCodeString()}"
    is Expr.MemberAccess -> "${expr.toCodeString(indent)}.$value"
    is Expr.SafeMemberAccess -> "${expr.toCodeString(indent)}?.$value"
    is Expr.ComponentAccess -> "${expr.toCodeString(indent)}.$value"
    is Expr.SafeComponentAccess -> "${expr.toCodeString(indent)}?.$value"
    is Expr.FunRef -> on.mapTo { it.toCodeString(indent) } + "::$value"
    is Expr.Spread -> "*${expr.toCodeString(indent)}"
    is Expr.Unary -> {
        val opS = op.toCodeString()
        val exprS = expr.toCodeString(indent)
        if (op.isPrefix()) "$opS($exprS)" else "($exprS)$opS"
    }
    is Expr.Binary -> "(${first.toCodeString(indent)} ${op.toCodeString()} ${second.toCodeString(indent)})"
    is Expr.And -> "${first.toCodeString(indent)} && ${second.toCodeString(indent)}"
    is Expr.Or -> "${first.toCodeString(indent)} || ${second.toCodeString(indent)}"
    is Expr.Elvis -> "${first.toCodeString(indent)} ?: ${second.toCodeString(indent)}"
    is Expr.Eq -> "${first.toCodeString(indent)} == ${second.toCodeString(indent)}"
    is Expr.NotEq -> "${first.toCodeString(indent)} != ${second.toCodeString(indent)}"
    is Expr.RefEq -> "${first.toCodeString(indent)} === ${second.toCodeString(indent)}"
    is Expr.NotRefEq -> "${first.toCodeString(indent)} !== ${second.toCodeString(indent)}"
    is Expr.Object -> TODO()
    is Expr.Get -> expr.toCodeString(indent) + "[${args.joinToString { it.toCodeString(indent) }}]"
}

fun BinaryOp.toCodeString() = when (this) {
    BinaryOp.Div -> "/"
    BinaryOp.GT -> ">"
    BinaryOp.GTEq -> ">="
    BinaryOp.LT -> "<"
    BinaryOp.LTEq -> "<="
    BinaryOp.Minus -> "-"
    BinaryOp.Plus -> "+"
    BinaryOp.RangeTo -> ".."
    BinaryOp.Rem -> "%"
    BinaryOp.Times -> "*"
}

fun UnaryOp.toCodeString() = when (this) {
    UnaryOp.Plus -> "+"
    UnaryOp.Minus -> "-"
    UnaryOp.Not -> "!"
    UnaryOp.NonNull -> "!!"
    UnaryOp.RangeFrom -> ".."
    UnaryOp.RangeUntil -> ".."
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