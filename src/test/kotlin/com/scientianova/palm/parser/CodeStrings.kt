package com.scientianova.palm.parser

import com.scientianova.palm.lexer.StringPart
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.AnnotationType.*
import com.scientianova.palm.parser.data.top.AnnotationType.Set
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.data.types.Enum
import com.scientianova.palm.parser.parsing.types.Constraints
import com.scientianova.palm.util.PString

fun <T> T?.mapTo(fn: (T) -> String) = if (this == null) "" else fn(this)

fun PExpr.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("typeToCodeString")
fun PType.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("decPatternToCodeString")
fun PDecPattern.toCodeString() = value.toCodeString()

@JvmName("typeArgToCodeString")
fun PTypeArg.toCodeString(indent: Int) = value.toCodeString(indent)

@JvmName("argsToCodeString")
fun List<Arg>.toCodeString(indent: Int) = "(${joinToString { it.toCodeString(indent) }})"

fun Annotation.toCodeString(indent: Int) =
    "@${type.toCodeString()}$path${args.toCodeString(indent)}"

@JvmName("annotationsToCodeString")
fun List<Annotation>.toCodeString(indent: Int) = joinToString(" ") { it.toCodeString(indent) }

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
    Constructor -> "constructor:"
}

fun Pattern.toCodeString(indent: Int): String = when (this) {
    is Pattern.Expr -> expr.toCodeString(indent)
    is Pattern.Type -> "is ${type.toCodeString(indent)}"
}

fun DecPattern.toCodeString(): String = when (this) {
    is DecPattern.Wildcard -> "_"
    is DecPattern.Name -> name
    is DecPattern.Tuple -> "(${elements.joinToString { it.toCodeString() }})"
}

fun Type.toCodeString(indent: Int): String = when (this) {
    is Type.Named -> path.path() + if (generics.isEmpty()) "" else "[${generics.joinToString { it.toCodeString(indent) }}]"
    is Type.Function -> "(${params.joinToString { it.toCodeString(indent) }}) -> ${returnType.toCodeString(indent)}"
    is Type.Nullable -> type.toCodeString(indent) + '?'
    is Type.Intersection -> types.joinToString(" + ") { it.toCodeString(indent) }
    is Type.Annotated -> annotation.toCodeString(indent) + " " + type.toCodeString(indent)
}

private fun typeAnn(type: PType?, indent: Int) = type.mapTo { ": ${it.toCodeString(indent)}" }

private fun eqExpr(expr: PExpr?, indent: Int) = expr.mapTo { " = ${it.toCodeString(indent)}" }

private fun eqType(type: PType?, indent: Int) = type.mapTo { " = ${it.toCodeString(indent)}" }

fun VarianceMod.toCodeString() = when (this) {
    VarianceMod.In -> "in "
    VarianceMod.Out -> "out "
    VarianceMod.None -> ""
}

fun TypeArg.toCodeString(indent: Int) = when (this) {
    is TypeArg.Normal -> variance.toCodeString() + type.toCodeString(indent)
    is TypeArg.Wildcard -> "_"
}

fun FunTypeArg.toCodeString(indent: Int) = (if (using) "using " else "") + type.toCodeString(indent)

fun Path.path() = joinToString(".") { it.value }

fun Import.toCodeString(): String = "import " + when (this) {
    is Import.Regular -> path + alias.mapTo { " as $it" }
    is Import.Package -> "${path.path()}._"
    is Import.Group -> "${start.path()}.{ ${members.joinToString { it.toCodeString() }} }"
}

fun DecModifier.toCodeString(indent: Int) = when (this) {
    DecModifier.Public -> "public"
    DecModifier.Protected -> "protected"
    DecModifier.Internal -> "internal"
    DecModifier.Private -> "private"
    DecModifier.Lateinit -> "lateinit"
    DecModifier.Inline -> "inline"
    DecModifier.Tailrec -> "tailrec"
    DecModifier.Ann -> "annotation"
    DecModifier.Abstract -> "abstract"
    DecModifier.Leaf -> "leaf"
    DecModifier.Partial -> "partial"
    DecModifier.Static -> "static"
    DecModifier.Override -> "override"
    DecModifier.Operator -> "operator"
    DecModifier.Blank -> "blank"
    DecModifier.In -> "in"
    DecModifier.Out -> "out"
    DecModifier.NoInline -> "noinline"
    DecModifier.CrossInline -> "crossinline"
    DecModifier.Using -> "using"
    is DecModifier.Annotation -> annotation.toCodeString(indent)
}


@JvmName("modifiersToCodeString")
fun List<DecModifier>.toCodeString(indent: Int) = joinToString(" ") { it.toCodeString(indent) } + if (isEmpty()) {
    ""
} else {
    " "
}

fun Condition.toCodeString(indent: Int): String = when (this) {
    is Condition.Expr -> expr.toCodeString(indent)
    is Condition.Pattern -> "${pattern.toCodeString(indent)} = ${expr.toCodeString(indent)}"
}

fun Function.toCodeString(indent: Int) = modifiers.toCodeString(indent) + "fun " +
        typeParams.typeParams() + (if (typeParams.isEmpty()) "" else " ") +
        "$name(${params.toCodeString(indent)})${typeAnn(type, indent)}" +
        funBody(expr, indent)

private fun funBody(expr: PExpr?, indent: Int) =
    expr.mapTo { (if (it.value is Expr.Scope) " " else " = ") + it.value.toCodeString(indent) }

fun Property.toCodeString(indent: Int) = when (this) {
    is Property.Normal -> modifiers.toCodeString(indent) + propertyType(mutable) + " $name" +
            typeAnn(type, indent) + eqExpr(expr, indent) +
            (if (getterModifiers.isEmpty() && getter == null) "" else '\n' + indent(indent + 1) +
                    getterModifiers.toCodeString(indent) + "get") + getter.toCodeString(indent) +
            (if (setterModifiers.isEmpty() && setter == null) "" else '\n' + indent(indent + 1) +
                    setterModifiers.toCodeString(indent) + "set") + setter.toCodeString(indent)
    is Property.Delegated -> modifiers.toCodeString(indent) + propertyType(mutable) + " $name" +
            typeAnn(type, indent) + " by ${delegate.toCodeString(indent)}"
}

fun Getter?.toCodeString(indent: Int) = mapTo { "()" + funBody(it.expr, indent) }

fun Setter?.toCodeString(indent: Int) = mapTo { "(${it.param.toCodeString(indent)})" + funBody(it.expr, indent) }

fun propertyType(mutable: Boolean) = if (mutable) "var" else "val"

fun SuperType.toCodeString(indent: Int) = when (this) {
    is SuperType.Class -> type.toCodeString(indent) + args.toCodeString(indent) + when (mixins.size) {
        0 -> ""
        1 -> "on ${mixins.first().toCodeString(indent)}"
        else -> "on (${mixins.joinToString { it.toCodeString(indent) }})"
    }
    is SuperType.Interface -> type.toCodeString(indent) + delegate.mapTo { "by $it" }
}

@JvmName("superTypesToCodeString")
fun List<SuperType>.toCodeString(indent: Int) = if (isEmpty()) "" else ": ${joinToString { it.toCodeString(indent) }} "

fun PrimaryParam.toCodeString(indent: Int) = modifiers.toCodeString(indent) + when (decHandling) {
    DecHandling.None -> ""
    DecHandling.Val -> "val "
    DecHandling.Var -> "var"
} + "$name: ${type.toCodeString(indent)}${eqExpr(default, indent)}}"

private fun Constraints.toCodeString(indent: Int) =
    if (isEmpty()) "" else "where " + joinToString { it.first.value + typeAnn(it.second, indent) } + " "

fun RecordProperty.toCodeString(indent: Int) =
    modifiers.toCodeString(indent) + name + typeAnn(type, indent) + eqExpr(default, indent)


fun Record.toCodeString(indent: Int) = when (this) {
    is Record.Tuple -> "${modifiers.toCodeString(indent)}record $name${typeParams.typeParams()}(${
        components.joinToString { it.toCodeString(indent) }
    })"
    is Record.Normal -> "${modifiers.toCodeString(indent)}record $name${typeParams.typeParams()} " +
            scopeCodeString(components, indent, ",") { it.toCodeString(indent + 1) }
    is Record.Single -> "${modifiers.toCodeString(indent)}record" + name.value
}

fun EnumCase.toCodeString(indent: Int) = when (this) {
    is EnumCase.Tuple -> "${annotations.toCodeString(indent)}$name(${components.joinToString { it.toCodeString(indent) }})"
    is EnumCase.Single -> name.value
}

fun Enum.toCodeString(indent: Int) = "${modifiers.toCodeString(indent)}enum $name${typeParams.typeParams()} " +
        scopeCodeString(cases, indent, ",") { it.toCodeString(indent + 1) }

fun Class.toCodeString(indent: Int) = modifiers.toCodeString(indent) + "class $name" +
        typeParams.toCodeString() +
        primaryConstructor.mapTo { "(${it.joinToString { param -> param.toCodeString(indent) }})" } + " " +
        superTypes.toCodeString(indent) +
        typeConstraints.toCodeString(indent) +
        scopeCodeString(statements, indent, "\n") { it.toCodeString(indent + 1) }

fun Object.toCodeString(indent: Int) = modifiers.toCodeString(indent) + "object $name " +
        superTypes.toCodeString(indent) +
        scopeCodeString(statements, indent, "\n") { it.toCodeString(indent + 1) }

fun ExtensionType.toCodeString(indent: Int) = type.toCodeString(indent) + alias.mapTo { " as $it" }

fun Extension.toCodeString(indent: Int): String = "extend " +
        typeParams.typeParams() + (if (typeParams.isEmpty()) "" else " ") +
        on.joinToString { it.toCodeString(indent) } + (if (on.isEmpty()) "" else " ") +
        typeConstraints.toCodeString(indent) +
        scopeCodeString(body, indent, "\n") { it.toCodeString(indent + 1) }

fun Mixin.toCodeString(indent: Int) = "${modifiers.toCodeString(indent)}mixin $name${typeParams.typeParams()} " +
        (if (on.isEmpty()) "" else "on " + on.joinToString { it.toCodeString(indent) } + " ") +
        typeConstraints.toCodeString(indent) +
        scopeCodeString(statements, indent, "\n") { it.toCodeString(indent + 1) }

fun Trait.toCodeString(indent: Int) = modifiers.toCodeString(indent) + "trait $name${typeParams.typeParams()} " +
        (if (superTraits.isEmpty()) "" else ": ") + superTraits.joinToString { it.toCodeString(indent) } +
        typeConstraints.toCodeString(indent) +
        scopeCodeString(statements, indent, "\n") { it.toCodeString(indent + 1) }

fun Implementation.toCodeString(indent: Int) = "impl " + when (this) {
    is Implementation.Inherent -> (if (typeParams.isEmpty()) "" else typeParams.typeParams() + " ") +
            type.toCodeString(indent) + " " +
            typeConstraints.toCodeString(indent) +
            scopeCodeString(statements, indent, "\n") { it.toCodeString(indent + 1) }
    is Implementation.Trait -> (if (typeParams.isEmpty()) "" else typeParams.typeParams() + " ") +
            "${trait.toCodeString(indent)} for ${on.toCodeString(indent)} " +
            typeConstraints.toCodeString(indent) +
            scopeCodeString(statements, indent, "\n") { it.toCodeString(indent + 1) }
}

fun ClassTypeParam.toCodeString() = variance.toCodeString() + type.value

fun List<PClassTypeParam>.toCodeString() = if (isEmpty()) "" else "[${joinToString { it.value.toCodeString() }}]"

fun List<PString>.typeParams() = if (isEmpty()) "" else "[${joinToString { it.value }}]"

fun FunParam.toCodeString(indent: Int) = modifiers.toCodeString(indent) +
        pattern.toCodeString() + typeAnn(type, indent) + eqExpr(default, indent)

fun OptionallyTypedFunParam.toCodeString(indent: Int) = modifiers.toCodeString(indent) +
        pattern.toCodeString() + typeAnn(type, indent) + eqExpr(default, indent)

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

@JvmName("conditionsToCodeString")
fun List<Condition>.toCodeString(indent: Int) = joinToString { it.toCodeString(indent) }

fun StringPart.toCodeString(indent: Int) = when (this) {
    is StringPart.String -> string
    is StringPart.Expr -> "$${expr.toCodeString(indent)}"
}

fun Expr.toCodeString(indent: Int): String = when (this) {
    is Expr.Ident -> name
    is Expr.Bool -> value.toString()
    is Expr.Null -> "null"
    is Expr.This -> "this"
    is Expr.Super -> "super"
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
            it.first.toCodeString(indent + 1) + " -> " + it.second.toCodeString(indent + 1)
        }
    }
${indent(indent)}}
""".trimIndent()
    is Expr.Lis -> "[${elements.joinToString { it.toCodeString(indent) }}]"
    is Expr.Map -> "[${
        if (elements.isEmpty()) ":" else elements.joinToString {
            "${it.first.toCodeString(indent)}: ${it.second.toCodeString(indent)}"
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
        val paramsString = params.joinToString { it.first.toCodeString() + typeAnn(it.second, indent) }
        """
{ $paramsString ->
${indent(indent + 1)}${scope.joinToString("\n" + indent(indent + 1)) { it.toCodeString(indent + 1) }}
${indent(indent)}}
        """.trimIndent()
    }
    is Expr.Tuple -> "(${elements.joinToString { it.toCodeString(indent) }})"
    is Expr.TypeCheck -> "${expr.toCodeString(indent)} is ${type.toCodeString(indent)}"
    is Expr.SafeCast -> "${expr.toCodeString(indent)} as ${type.toCodeString(indent)}"
    is Expr.NullableCast -> "${expr.toCodeString(indent)} as? ${type.toCodeString(indent)}"
    is Expr.UnsafeCast -> "${expr.toCodeString(indent)} as! ${type.toCodeString(indent)}"
    is Expr.MemberAccess -> "${expr.toCodeString(indent)}.$value"
    is Expr.SafeMemberAccess -> "${expr.toCodeString(indent)}?.$value"
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
    is Expr.Object -> "object ${superTypes.toCodeString(indent)}" +
            scopeCodeString(statements, indent) { it.toCodeString(indent + 1) }
    is Expr.Get -> expr.toCodeString(indent) + "[${args.joinToString { it.toCodeString(indent) }}]"
    is Expr.Throw -> "throw ${expr.toCodeString(indent)}"
    is Expr.Do -> "do ${scope.toCodeString(indent)}" + if (catches.isEmpty()) "" else " " + catches.joinToString(" ") {
        it.toCodeString(
            indent
        )
    }
    is Expr.Turbofish -> ".${expr.toCodeString(indent)}[${args.toCodeString(indent)}]"
    is Expr.Annotated -> annotation.toCodeString(indent) + " " + expr.toCodeString(indent)
    Expr.Wildcard -> "_"
    is Expr.Dec -> "${if (mutable) "val" else "var"} ${pattern.toCodeString(indent)}" + typeAnn(type, indent) +
            expr.mapTo { " = ${it.toCodeString(indent)}" }
    is Expr.Assign -> "${left.toCodeString(indent)} ${type.toCodeString()} ${right.toCodeString(indent)}"
    is Expr.Guard -> "guard ${cond.toCodeString(indent)} else ${body.toCodeString(indent)}"
    is Expr.Defer -> "defer ${body.toCodeString(indent)}"
}


@JvmName("typeArgsToCodeString")
fun List<PTypeArg>.toCodeString(indent: Int) = "[${joinToString { it.toCodeString(indent) }}]"

fun Catch.toCodeString(indent: Int) =
    "catch ${dec.toCodeString()}: ${type.toCodeString(indent)} ${body.toCodeString(indent)}"

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

private fun init(scope: ExprScope, indent: Int) = "init " + scope.toCodeString(indent)

fun ClassStatement.toCodeString(indent: Int) = when (this) {
    is ClassStatement.Constructor -> modifiers.toCodeString(indent) + "constructor" + params.toCodeString(indent) + " " +
            primaryCall.mapTo { ": this${it.toCodeString(indent)} " } + body.toCodeString(indent)
    is ClassStatement.Initializer -> init(scope, indent)
    is ClassStatement.Method -> function.toCodeString(indent)
    is ClassStatement.Property -> property.toCodeString(indent)
}

fun ObjectStatement.toCodeString(indent: Int) = when (this) {
    is ObjectStatement.Initializer -> init(scope, indent)
    is ObjectStatement.Method -> function.toCodeString(indent)
    is ObjectStatement.Property -> property.toCodeString(indent)
}

fun ExtensionStatement.toCodeString(indent: Int) = when (this) {
    is ExtensionStatement.Extension -> extension.toCodeString(indent)
    is ExtensionStatement.Method -> function.toCodeString(indent)
    is ExtensionStatement.Property -> property.toCodeString(indent)
}

fun MixinStatement.toCodeString(indent: Int) = when (this) {
    is MixinStatement.Method -> function.toCodeString(indent)
    is MixinStatement.Property -> property.toCodeString(indent)
}

fun TraitStatement.toCodeString(indent: Int) = when (this) {
    is TraitStatement.Method -> function.toCodeString(indent)
    is TraitStatement.Property -> property.toCodeString(indent)
    is TraitStatement.AssociatedType -> "type $name${typeAnn(bound, indent)}${eqType(default, indent)}"
}

fun ImplStatement.toCodeString(indent: Int) = when (this) {
    is ImplStatement.Method -> function.toCodeString(indent)
    is ImplStatement.Property -> property.toCodeString(indent)
}

fun TraitImplStatement.toCodeString(indent: Int) = when (this) {
    is TraitImplStatement.Method -> function.toCodeString(indent)
    is TraitImplStatement.Property -> property.toCodeString(indent)
    is TraitImplStatement.AssociatedType -> "type $name = ${type.toCodeString(indent)}"
}

fun FileStatement.toCodeString(indent: Int) = when (this) {
    is StaticFunction -> function.toCodeString(indent)
    is StaticProperty -> property.toCodeString(indent)
    is StaticClass -> clazz.toCodeString(indent)
    is StaticRecord -> record.toCodeString(indent)
    is StaticEnum -> enum.toCodeString(indent)
    is StaticObject -> obj.toCodeString(indent)
    is StaticExtension -> extension.toCodeString(indent)
    is StaticImpl -> implementation.toCodeString(indent)
    is StaticTrait -> trait.toCodeString(indent)
    is StaticMixin -> mixin.toCodeString(indent)
    is TypeAlias -> modifiers.toCodeString(indent) + "type $name${params.typeParams()} = " + actual.toCodeString(indent)
    is Constant -> modifiers.toCodeString(indent) + "const $name" + typeAnn(type, indent) + eqExpr(expr, indent)
}

fun FileScope.toCodeString(indent: Int) = metadataComments.joinToString("\n") { "$$it" } +
        annotations.joinToString("\n") { it.toCodeString(indent) } +
        "package ${path.path()}\n\n" +
        imports.joinToString("\n") { it.toCodeString() } + "\n\n" +
        statements.joinToString("\n\n") { it.toCodeString(indent) }