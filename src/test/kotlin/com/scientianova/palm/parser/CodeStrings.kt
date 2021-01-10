package com.scientianova.palm.parser

import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.AnnotationType.*
import com.scientianova.palm.parser.data.top.AnnotationType.Set
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.util.PString

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
    AnnotationType.Property -> "property:"
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
            else "[${generics.joinToString { it.toCodeString { type -> type.toCodeString(indent) } }}]"
    is Type.Function -> "(${params.joinToString { it.toCodeString(indent) }}) -> ${returnType.toCodeString(indent)}"
    is Type.Nullable -> type.toCodeString(indent) + '?'
    is Type.Annotated -> annotation.value.toCodeString(indent) + " " + type.toCodeString(indent)
    Type.Infer -> "_"
    is Type.Lis -> "[${type.toCodeString(indent)}]"
    is Type.Dict -> "[${key.toCodeString(indent)}:${value.toCodeString(indent)}]"
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

fun Import.toCodeString(): String = "import " + when (this) {
    is Import.Regular -> path + alias.mapTo { " as $it" }
    is Import.Package -> "${path.path()}._"
    is Import.Group -> "${start.path()}.{ ${members.joinToString { it.toCodeString() }} }"
}

fun DecModifier.toCodeString(indent: Int): String = when (this) {
    DecModifier.Public -> "public"
    DecModifier.Protected -> "protected"
    DecModifier.Internal -> "internal"
    DecModifier.Private -> "private"
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
    DecModifier.Enum -> "enum"
    DecModifier.Sealed -> "sealed"
    DecModifier.Data -> "data"
    DecModifier.NoInline -> "noinline"
    DecModifier.CrossInline -> "crossinline"
}

@JvmName("decModifiersToCodeString")
fun List<PDecMod>.toCodeString(indent: Int): String =
    joinToString(" ") { it.value.toCodeString(indent) } + if (isEmpty()) {
        ""
    } else {
        " "
    }

fun Function.toCodeString(indent: Int) = modifiers.toCodeString(indent) + "fun $name" +
        typeParams.typeParams() +
        context.toCodeString(indent) +
        "(${params.toCodeString(indent)})" +
        typeAnn(type, indent) +
        constraints.toCodeString(indent) +
        funBody(expr, indent)

private fun funBody(expr: PExpr?, indent: Int) =
    expr.mapTo { (if (it.value is Expr.Scope) " " else " = ") + it.value.toCodeString(indent) }

fun Property.toCodeString(indent: Int) = modifiers.toCodeString(indent) + propertyType(mutable) + " $name" +
        context.toCodeString(indent) + typeAnn(type, indent) + when (val bod = body) {
    is PropertyBody.Normal -> eqExpr(bod.expr, indent) +
            (if (bod.getterModifiers.isEmpty() && bod.getter == null) "" else '\n' + indent(indent + 1) +
                    bod.getterModifiers.toCodeString(indent) + "get") + bod.getter.toCodeString(indent) +
            (if (bod.setterModifiers.isEmpty() && bod.setter == null) "" else '\n' + indent(indent + 1) +
                    bod.setterModifiers.toCodeString(indent) + "set") + bod.setter.toCodeString(indent)
    is PropertyBody.Delegate -> " by ${bod.expr.toCodeString(indent)}"
}

fun Getter?.toCodeString(indent: Int) = mapTo { "()" + typeAnn(it.type, indent) + funBody(it.expr, indent) }

fun Setter?.toCodeString(indent: Int) =
    mapTo { "(${it.param.toCodeString(indent)})" + typeAnn(it.type, indent) + funBody(it.expr, indent) }

fun propertyType(mutable: Boolean) = if (mutable) "var" else "val"

fun SuperType.toCodeString(indent: Int) = when (this) {
    is SuperType.Class -> type.toCodeString(indent) + args.toCodeString(indent)
    is SuperType.Interface -> type.toCodeString(indent) + delegate.mapTo { "by $it" }
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

fun Class.toCodeString(indent: Int): String = modifiers.toCodeString(indent) + "class $name" +
        typeParams.toCodeString() +
        primaryConstructor.mapTo { "(${it.joinToString { param -> param.toCodeString(indent) }})" } + " " +
        superTypes.toCodeString(indent) +
        typeConstraints.toCodeString(indent) +
        scopeCodeStringOrNone(statements, indent) { it.toCodeString(indent + 1) }

fun Object.toCodeString(indent: Int): String = modifiers.toCodeString(indent) + "object $name " +
        superTypes.toCodeString(indent) +
        scopeCodeStringOrNone(statements, indent) { it.toCodeString(indent + 1) }

fun Interface.toCodeString(indent: Int): String = modifiers.toCodeString(indent) + "interface $name " +
        superTypes.joinToString { it.toCodeString(indent) } +
        scopeCodeStringOrNone(items, indent) { it.toCodeString(indent + 1) }

fun TypeClass.toCodeString(indent: Int): String =
    modifiers.toCodeString(indent) + "type class $name${typeParams.typeParams()} " +
            (if (superTypes.isEmpty()) "" else ": ") + superTypes.joinToString { it.toCodeString(indent) } +
            typeConstraints.toCodeString(indent) +
            scopeCodeStringOrNone(items, indent) { it.toCodeString(indent + 1) }

fun Implementation.toCodeString(indent: Int) =
    "impl " + (if (typeParams.isEmpty()) "" else typeParams.typeParams() + " ") +
            type.toCodeString(indent) +
            context.toCodeString(indent) +
            typeConstraints.toCodeString(indent) +
            scopeCodeStringOrNone(items, indent) { it.toCodeString(indent + 1) }

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
    is Expr.Object -> "object ${superTypes.toCodeString(indent)}" +
            scopeCodeString(statements, indent) { it.toCodeString(indent + 1) }
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

fun ClassStmt.toCodeString(indent: Int) = when (this) {
    is ClassStmt.Constructor -> modifiers.toCodeString(indent) + "constructor" + params.toCodeString(indent) + " " +
            primaryCall.mapTo { ": this${it.toCodeString(indent)} " } + body.mapTo { it.toCodeString(indent) }
    is ClassStmt.Initializer -> init(scope, indent)
    is ClassStmt.Item -> item.toCodeString(indent)
}

fun ObjStmt.toCodeString(indent: Int) = when (this) {
    is ObjStmt.Initializer -> init(scope, indent)
    is ObjStmt.Item -> item.toCodeString(indent)
}

fun FileStmt.toCodeString(indent: Int) = when (this) {
    is FileStmt.Init -> init(scope, indent)
    is FileStmt.Item -> item.toCodeString(indent)
}

fun TypeAlias.toCodeString(indent: Int) = modifiers.toCodeString(indent) + "type $name${params.typeParams()}" +
        typeBound(bound, indent) + eqType(actual, indent)

fun ItemKind.toCodeString(indent: Int) = when (this) {
    is ItemKind.Prop -> property.toCodeString(indent)
    is ItemKind.Fun -> function.toCodeString(indent)
    is ItemKind.Clazz -> clazz.toCodeString(indent)
    is ItemKind.Inter -> inter.toCodeString(indent)
    is ItemKind.Obj -> obj.toCodeString(indent)
    is ItemKind.TC -> tc.toCodeString(indent)
    is ItemKind.Impl -> impl.toCodeString(indent)
    is ItemKind.Alias -> alias.toCodeString(indent)
}

fun Item.toCodeString(indent: Int): String = kind.toCodeString(indent)

fun ScopeStmt.toCodeString(indent: Int): String = when (this) {
    is ScopeStmt.Expr -> value.toCodeString(indent)
    is ScopeStmt.Defer -> "defer " + body.toCodeString(indent)
    is ScopeStmt.Imp -> import.toCodeString()
    is ScopeStmt.Dec -> propertyType(mutable) + " " + pattern.toCodeString() +
            typeAnn(type, indent) + eqExpr(expr, indent)
}

fun ContextParam.toCodeString(indent: Int) =
    modifiers.toCodeString(indent) +
            (if (pattern.value is DecPattern.Wildcard) "" else pattern.toCodeString() + ": ") +
            type.toCodeString(indent)

@JvmName("contextToCodeString")
fun List<ContextParam>.toCodeString(indent: Int) =
    if (isEmpty()) "" else "[" + joinToString { it.toCodeString(indent) } + "]"

fun FileScope.toCodeString(indent: Int) =
    annotations.joinToString("\n") { it.toCodeString(indent) } + (if (annotations.isEmpty()) "" else "\n") +
            imports.joinToString("\n") { it.toCodeString() } + (if (imports.isEmpty()) "" else "\n\n") +
            statements.joinToString("\n\n") { it.toCodeString(indent) }