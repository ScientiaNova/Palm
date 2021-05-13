package com.scientianova.palm.parser

import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.AnnotationType.*
import com.scientianova.palm.parser.data.top.AnnotationType.Set
import com.scientianova.palm.queries.FileId
import com.scientianova.palm.queries.fileIdToParsed
import com.scientianova.palm.queries.fileToItems
import com.scientianova.palm.queries.itemIdToParsedKind
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Path
import com.scientianova.palm.util.PathType

fun <T> T?.mapTo(fn: (T) -> String) = if (this == null) "" else fn(this)

fun PExpr.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("typeToCodeString")
fun PType.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("decPatternToCodeString")
fun PDecPattern.toCodeString() = value.toCodeString()

@JvmName("nestedTypeToCodeString")
fun PNestedType.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("annotationToCodeString")
fun Annotation.toCodeString(indent: Int) =
    "@${type.toCodeString()}$path${args.toCodeString(indent)}"

@JvmName("pExprScopeToCodeString")
fun PExprScope.toCodeString(indent: Int) = scopeCodeString(this.value, indent) { it.toCodeString(indent + 1) }

fun AnnotationType.toCodeString() = when (this) {
    Normal -> ""
    Get -> "val:"
    Set -> "set:"
    File -> "file:"
    Field -> "field:"
    Delegate -> "delegate"
    Property -> "property:"
    Param -> "param:"
    SetParam -> "setparam:"
    Init -> "init"
}

fun Pattern.toCodeString(indent: Int): String = when (this) {
    is Pattern.Expr -> expr.toCodeString(indent)
    is Pattern.Type -> "is ${type.toCodeString(indent)}" + destructuring.mapTo { it.toCodeString(indent) }
    is Pattern.Tuple -> "(${patterns.joinToString { it.toCodeString(indent) }})"
    is Pattern.In -> "in ${expr.toCodeString(indent)}"
    is Pattern.Dec -> propertyType(mutable) + " " + decPattern.toCodeString()
    Pattern.Wildcard -> "_"
}

fun BranchRes.toCodeString(indent: Int) = when (this) {
    is BranchRes.Branching -> {
        " when ${on.mapTo { "(${it.toCodeString(indent)})" }} " +
                scopeCodeString(branches, indent) { it.toCodeString(indent + 1) }
    }
    is BranchRes.Single -> " -> ${expr.toCodeString(indent)}"
}

fun WhenBranch.toCodeString(indent: Int): String =
    pattern.toCodeString(indent) +
            guard.mapTo { " if ${it.expr.toCodeString(indent)}" } +
            res.toCodeString(indent)

fun DecPattern.toCodeString(): String = when (this) {
    is DecPattern.Wildcard -> "_"
    is DecPattern.Name -> name
    is DecPattern.Components -> "(${elements.joinToString { it.toCodeString() }})"
    is DecPattern.Object -> elements.joinToString { it.first.value + it.second.mapTo { nested -> ": " + nested.toCodeString() } }
}

fun Type.toCodeString(indent: Int): String = when (this) {
    is Type.Named -> path.path() +
            if (generics.isEmpty()) ""
            else "<${generics.joinToString { it.toCodeString { type -> type.toCodeString(indent) } }}>"
    is Type.Function -> "(${params.joinToString { it.toCodeString(indent) }}) -> ${returnType.toCodeString(indent)}"
    is Type.Nullable -> type.toCodeString(indent) + '?'
    is Type.Annotated -> annotation.value.toCodeString(indent) + " " + type.toCodeString(indent)
    Type.Infer -> "_"
    is Type.Lis -> "[${type.toCodeString(indent)}]"
    is Type.Dict -> "[${key.toCodeString(indent)}: ${value.toCodeString(indent)}]"
    is Type.Tuple -> "(${types.joinToString { it.toCodeString(indent) }})"
}

private fun typeAnn(type: PType?, indent: Int) = type.mapTo { ": ${it.toCodeString(indent)}" }

private fun eqExpr(expr: PExpr?, indent: Int) = expr.mapTo { " = ${it.toCodeString(indent)}" }

private fun eqType(type: PType?, indent: Int) = type.mapTo { " = ${it.toCodeString(indent)}" }

fun VarianceMod.toCodeString() = when (this) {
    VarianceMod.In -> "in "
    VarianceMod.Out -> "out "
    VarianceMod.None -> ""
}

fun NestedType.toCodeString(indent: Int) = when (this) {
    is NestedType.Normal -> variance.toCodeString() + type.toCodeString(indent)
    is NestedType.Wildcard -> "_"
}

fun Path.path() = joinToString(".") { it.value }

fun PathType.toCodeString(): String = when (this) {
    PathType.Module -> "mod."
    PathType.Crate -> "crate."
    PathType.Super -> "super."
    PathType.Root -> ""
}

fun Import.toCodeString(): String = "import " + pathType.toCodeString() +
        (if (pathType != PathType.Root && body !is ImportBody.Group && body.path.isNotEmpty()) "." else "") +
        body.toCodeString()

fun ImportBody.toCodeString(): String = when (this) {
    is ImportBody.Qualified -> path.path() + " as " + alias.value
    is ImportBody.File -> path.path()
    is ImportBody.Group -> "${path.path()}.{ ${
        members.joinToString {
            if (it is ImportBody.File && it.path.isEmpty()) "mod" else it.toCodeString()
        }
    } }"
    is ImportBody.Show -> "${path.path()} show { ${items.joinToString { it.first.value + it.second.mapTo { alias -> " as $alias" } }} }"
    is ImportBody.Hide -> "${path.path()} hide { ${items.joinToString { it.value }} }"
}

fun DecModifier.toCodeString(indent: Int): String = when (this) {
    DecModifier.Public -> "public"
    DecModifier.Protected -> "protected"
    is DecModifier.Private -> "private" + if (pathType == PathType.Root && path.isEmpty()) "" else "(${
        pathType.toCodeString() + (if (pathType != PathType.Root && path.isNotEmpty()) "." else "") + path.path()
    })"
    DecModifier.Lateinit -> "lateinit"
    DecModifier.Inline -> "inline"
    DecModifier.Ann -> "annotation"
    DecModifier.Abstract -> "abstract"
    DecModifier.Override -> "override"
    DecModifier.Operator -> "operator"
    is DecModifier.Annotation -> annotation.toCodeString(indent)
    DecModifier.Open -> "open"
    DecModifier.Final -> "final"
    DecModifier.Const -> "const"
    is DecModifier.Sealed -> "sealed" + if (pathType == PathType.Root && path.isEmpty()) "" else "(${
        pathType.toCodeString() + (if (pathType != PathType.Root && path.isNotEmpty()) "." else "") + path.path()
    })"
    DecModifier.Data -> "data"
    DecModifier.NoInline -> "noinline"
    DecModifier.CrossInline -> "crossinline"
    DecModifier.Leaf -> "leaf"
}

@JvmName("decModifiersToCodeString")
fun List<PDecMod>.toCodeString(indent: Int): String =
    joinToString(" ") { it.value.toCodeString(indent) } + if (isEmpty()) {
        ""
    } else {
        " "
    }

private fun funBody(expr: PExpr?, indent: Int) =
    expr.mapTo { (if (it.value is Expr.Scope) " " else " = ") + it.value.toCodeString(indent) }

fun Getter?.toCodeString(indent: Int) = mapTo { "()" + typeAnn(it.type, indent) + funBody(it.expr, indent) }

fun Setter?.toCodeString(indent: Int) =
    mapTo { "(${it.param.toCodeString(indent)})" + typeAnn(it.type, indent) + funBody(it.expr, indent) }

fun propertyType(mutable: Boolean) = if (mutable) "var" else "val"

fun SuperType.toCodeString(indent: Int) = when (this) {
    is SuperType.Class -> type.toCodeString(indent) + args.toCodeString(indent)
    is SuperType.Interface -> type.toCodeString(indent)
}

@JvmName("superTypesToCodeString")
fun List<PSuperType>.toCodeString(indent: Int) =
    if (isEmpty()) "" else ": ${joinToString { it.value.toCodeString(indent) }} "

fun PrimaryParam.toCodeString(indent: Int): String = modifiers.toCodeString(indent) + when (decHandling) {
    DecHandling.None -> ""
    DecHandling.Val -> "val "
    DecHandling.Var -> "var "
} + "$name: ${type.toCodeString(indent)}${eqExpr(default, indent)}"

private fun TypeConstraints.toCodeString(indent: Int) = if (isEmpty()) "" else " where " +
        joinToString { it.first.value + typeBound(it.second, indent) }

fun typeBound(types: List<PType>, indent: Int) =
    if (types.isEmpty()) "" else ": " + types.joinToString(" + ") { it.toCodeString(indent) }

fun ClassTypeParam.toCodeString() = variance.toCodeString() + type.value

fun List<PClassTypeParam>.toCodeString() = if (isEmpty()) "" else "<${joinToString { it.value.toCodeString() }}>"

fun List<PString>.typeParams() = if (isEmpty()) "" else "<${joinToString { it.value }}>"

fun FunParam.toCodeString(indent: Int) = modifiers.toCodeString(indent) +
        pattern.toCodeString() + typeAnn(type, indent) + eqExpr(default, indent)

fun OptionallyTypedFunParam.toCodeString(indent: Int) = modifiers.toCodeString(indent) +
        pattern.toCodeString() + typeAnn(type, indent) + eqExpr(default, indent)

@JvmName("funParamsToCodeString")
fun List<FunParam>.toCodeString(indent: Int) = joinToString { it.toCodeString(indent) }

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

private inline fun <T> scopeCodeStringOrNone(
    list: List<T>,
    indent: Int,
    separator: String = "",
    crossinline fn: (T) -> String
) = if (list.isEmpty()) "" else scopeCodeString(list, indent, separator, fn)

@JvmName("scopeToCodeString")
fun ExprScope.toCodeString(indent: Int) = scopeCodeString(this, indent) { it.toCodeString(indent + 1) }

fun indent(count: Int) = "    ".repeat(count)

fun StringPartP.toCodeString(indent: Int) = when (this) {
    is StringPartP.String -> string
    is StringPartP.Expr -> "$${expr.toCodeString(indent)}"
}

fun Expr.toCodeString(indent: Int): String = when (this) {
    is Expr.Ident -> name
    is Expr.Bool -> value.toString()
    is Expr.Null -> "null"
    is Expr.Super -> "super"
    is Expr.Byte -> value.toString()
    is Expr.Short -> value.toString()
    is Expr.Int -> value.toString()
    is Expr.Long -> value.toString()
    is Expr.Float -> value.toString()
    is Expr.Double -> value.toString()
    is Expr.Char -> "'$value'"
    is Expr.Str -> "\"${parts.joinToString("") { it.toCodeString(indent) }}\""
    is Expr.If -> "if (${cond.toCodeString(indent)}) ${ifTrue.toCodeString(indent)}" +
            ifFalse.mapTo { " else ${it.toCodeString(indent)}" }
    is Expr.Scope -> scope.toCodeString(indent)
    is Expr.When -> "when " + comparing.mapTo { "(${it.toCodeString(indent)}) " } +
            scopeCodeString(branches, indent) { it.toCodeString(indent + 1) }
    is Expr.Lis -> "[${elements.joinToString { it.toCodeString(indent) }}]"
    is Expr.Map -> "[${
        if (elements.isEmpty()) ":" else elements.joinToString {
            "${it.first.toCodeString(indent)}: ${it.second.toCodeString(indent)}"
        }
    }]"
    is Expr.Break -> "break" + label.mapTo { "@$it" } + expr.mapTo { " ${it.toCodeString(indent)}" }
    is Expr.Return -> "return" + label.mapTo { "@$it" } + expr.mapTo { " ${it.toCodeString(indent)}" }
    is Expr.Call -> expr.toCodeString(indent) + args.toCodeString(indent)
    is Expr.Lambda ->
        """
{ ${params.mapTo { it.toCodeString(indent) }}
${indent(indent + 1)}${scope.joinToString("\n" + indent(indent + 1)) { it.toCodeString(indent + 1) }}
${indent(indent)}}
        """.trimIndent()
    is Expr.Tuple -> "(${elements.joinToString { it.toCodeString(indent) }})"
    is Expr.TypeCheck -> "${expr.toCodeString(indent)} is ${type.toCodeString(indent)}" +
            destructuring.mapTo { it.toCodeString(indent) }
    is Expr.NullableCast -> "${expr.toCodeString(indent)} as ${type.toCodeString(indent)}"
    is Expr.TypeInfo -> "${expr.toCodeString(indent)} as? ${type.toCodeString(indent)}"
    is Expr.UnsafeCast -> "${expr.toCodeString(indent)} as! ${type.toCodeString(indent)}"
    is Expr.MemberAccess -> "${expr.toCodeString(indent)}.$value"
    is Expr.Safe -> when (val nested = expr) {
        is Expr.MemberAccess -> "${nested.expr.toCodeString(indent)}?.${nested.value}"
        is Expr.Get -> nested.expr.toCodeString(indent) + "?[${nested.args.joinToString { it.toCodeString(indent) }}]"
        is Expr.Call -> nested.expr.toCodeString(indent) + '?' + nested.args.toCodeString(indent)
        else -> error("Impossible")
    }
    is Expr.FunRef -> on.mapTo { it.toCodeString(indent) } + "::$value"
    is Expr.Spread -> "*${expr.toCodeString(indent)}"
    is Expr.Get -> expr.toCodeString(indent) + "[${args.joinToString { it.toCodeString(indent) }}]"
    is Expr.Throw -> "throw ${expr.toCodeString(indent)}"
    is Expr.Do -> "do ${scope.toCodeString(indent)}" + if (catches.isEmpty()) "" else " " + catches.joinToString(" ") {
        it.toCodeString(indent)
    }
    is Expr.Turbofish -> ".${expr.toCodeString(indent)}[${args.toCodeString { it.toCodeString(indent) }}]"
    Expr.Error -> "!!!error!!!"
    is Expr.ContextCall -> ".[${args.toCodeString(indent)}]"
    is Expr.Unary -> op.value.toCodeString(expr.toCodeString(indent))
    is Expr.Binary -> first.toCodeString(indent) + op.value.toCodeString() + second.toCodeString(indent)
}

fun Destructuring.toCodeString(indent: Int) = when (this) {
    is Destructuring.Components -> "(${components.joinToString { it.toCodeString(indent) }})"
    is Destructuring.Object -> "{${properties.joinToString { "${it.first}: ${it.second.toCodeString(indent)}" }}}"
}

fun LambdaParams.toCodeString(indent: Int) =
    (if (context.isEmpty()) "" else "[${
        context.joinToString {
            it.first.toCodeString() + typeAnn(
                it.second,
                indent
            )
        }
    }]") +
            "(${explicit.joinToString { it.first.toCodeString() + typeAnn(it.second, indent) }}) ->"

fun UnOp.toCodeString(expr: String) = when (this) {
    UnOp.Not -> "!$expr"
    UnOp.Plus -> "+$expr"
    UnOp.Minus -> "-$expr"
    UnOp.NonNull -> "$expr!"
}

fun ExprOp.toCodeString() = when (this) {
    Times -> " * "
    Div -> " / "
    Rem -> " % "
    Plus -> " + "
    Minus -> " - "
    RangeTo -> ".."
    is Infix -> " $name "
    Elvis -> " ?: "
    In -> " in "
    Less -> " < "
    Greater -> " > "
    LessOrEq -> " <= "
    GreaterOrEq -> " >= "
    Eq -> " == "
    RefEq -> " === "
    And -> " && "
    Or -> " || "
    Assign -> " = "
    PlusAssign -> " += "
    MinusAssign -> " -= "
    TimesAssign -> " *= "
    DivAssign -> " /= "
    RemAssign -> " %= "
}

fun <T> Arg<T>.toCodeString(fn: (T) -> String) = name.mapTo { "$it: " } + fn(value)

@JvmName("argsToCodeString")
fun <T> List<Arg<T>>.toCodeString(fn: (T) -> String) = joinToString { it.toCodeString(fn) }

@JvmName("exprArgsToCodeString")
fun List<Arg<PExpr>>.toCodeString(indent: Int) = toCodeString { it.toCodeString(indent) }

fun Catch.toCodeString(indent: Int) =
    "catch ${dec.toCodeString()}: ${type.toCodeString(indent)} ${body.toCodeString(indent)}"

fun CallArgs.toCodeString(indent: Int): String =
    (if (args.isEmpty() && trailing.isNotEmpty()) "" else "(${args.toCodeString(indent)})") +
            (if (trailing.isEmpty()) "" else " ") +
            trailing.joinToString(" ") { it.toCodeString { expr -> expr.toCodeString(indent) } }

private fun init(scope: PExprScope, indent: Int) = "init " + scope.toCodeString(indent)

fun Constructor.toCodeString(indent: Int) =
    modifiers.toCodeString(indent) + "constructor" + params.toCodeString(indent) + " " +
            primaryCall.mapTo { ": this${it.toCodeString(indent)} " } + body.mapTo { it.toCodeString(indent) }

fun ItemKind.toCodeString(indent: Int): String = when (this) {
    is ItemKind.Property -> modifiers.toCodeString(indent) + propertyType(mutable) + " $name" +
            context.contextParams(indent) + typeAnn(type, indent) + eqExpr(expr, indent) +
            (if (getterModifiers.isEmpty() && getter == null) "" else '\n' + indent(indent + 1) +
                    getterModifiers.toCodeString(indent) + "get") + getter.toCodeString(indent) +
            (if (setterModifiers.isEmpty() && setter == null) "" else '\n' + indent(indent + 1) +
                    setterModifiers.toCodeString(indent) + "set") + setter.toCodeString(indent)

    is ItemKind.Function -> modifiers.toCodeString(indent) + "fun $name" +
            typeParams.typeParams() +
            context.contextParams(indent) +
            "(${params.toCodeString(indent)})" +
            typeAnn(type, indent) +
            constraints.toCodeString(indent) +
            funBody(expr, indent)
    is ItemKind.Class -> modifiers.toCodeString(indent) + "class $name" +
            typeParams.toCodeString() +
            primaryConstructor.mapTo { "(${it.joinToString { param -> param.toCodeString(indent) }})" } + " " +
            superTypes.toCodeString(indent) +
            typeConstraints.toCodeString(indent) +
            scopeCodeStringOrNone(items, indent) { itemIdToParsedKind[it]!!.toCodeString(indent + 1) }
    is ItemKind.Interface -> modifiers.toCodeString(indent) + "interface $name " +
            superTypes.joinToString { it.toCodeString(indent) } +
            scopeCodeStringOrNone(items, indent) { itemIdToParsedKind[it]!!.toCodeString(indent + 1) }
    is ItemKind.Object -> modifiers.toCodeString(indent) + "object $name " +
            superTypes.toCodeString(indent) +
            scopeCodeStringOrNone(statements, indent) { itemIdToParsedKind[it]!!.toCodeString(indent + 1) }
    is ItemKind.TypeClass ->
        modifiers.toCodeString(indent) + "type class $name${typeParams.typeParams()} " +
                (if (superTypes.isEmpty()) "" else ": ") + superTypes.joinToString { it.toCodeString(indent) } +
                typeConstraints.toCodeString(indent) +
                scopeCodeStringOrNone(items, indent) { itemIdToParsedKind[it]!!.toCodeString(indent + 1) }
    is ItemKind.Implementation ->
        "impl " + (if (typeParams.isEmpty()) "" else typeParams.typeParams() + " ") +
                type.toCodeString(indent) +
                context.contextParams(indent) +
                typeConstraints.toCodeString(indent) +
                scopeCodeStringOrNone(items, indent) { itemIdToParsedKind[it]!!.toCodeString(indent + 1) }
    is ItemKind.TypeAlias -> modifiers.toCodeString(indent) + "type $name${params.typeParams()}" +
            typeBound(bound, indent) + eqType(actual, indent)
    is ItemKind.Initializer -> init(scope, indent)
}

fun ScopeStmt.toCodeString(indent: Int): String = when (this) {
    is ScopeStmt.Expr -> value.toCodeString(indent)
    is ScopeStmt.Defer -> "defer " + body.toCodeString(indent)
    is ScopeStmt.Dec -> propertyType(mutable) + " " + pattern.toCodeString() +
            typeAnn(type, indent) + eqExpr(expr, indent)
}

fun List<FunParam>.contextParams(indent: Int) =
    if (isEmpty()) "" else "[" + joinToString { it.toCodeString(indent) } + "]"

fun FileScope.toCodeString(indent: Int) =
    annotations.joinToString("\n") { it.toCodeString(indent) } + (if (annotations.isEmpty()) "" else "\n") +
            imports.joinToString("\n") { it.toCodeString() } + (if (imports.isEmpty()) "" else "\n\n")

fun FileId.toCodeString(indent: Int) = fileIdToParsed[this]!!.toCodeString(indent) +
        fileToItems[this]!!.joinToString("\n\n") { itemIdToParsedKind[it]!!.toCodeString(indent) }