package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.CallArgs
import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.parser.data.top.TopLevelPrivacy
import com.scientianova.palm.util.PString

enum class ClassLevelPrivacy {
    Public, Protected, Private
}

enum class ClassImplementation {
    Leaf, Full, Abstract
}

data class SuperClass(val type: PType, val args: CallArgs, val mixins: List<PType>)

data class Class(
    val name: PString,
    val privacy: TopLevelPrivacy,
    val implementation: ClassImplementation,
    val primaryConstructor: List<FunParam>,
    val typeParams: List<PClassTypeParam>,
    val typeConstraints: TypeConstraints,
    val superClass: SuperClass?,
    val implements: List<PType>,
    val statements: List<ClassStatement>
)

data class ClassPropertyInfo(
    val privacy: ClassLevelPrivacy,
    val override: Boolean,
    val lateInit: Boolean
)

data class MethodInfo(
    val privacy: ClassLevelPrivacy,
    val operator: Boolean,
    val blank: Boolean,
    val override: Boolean,
    val tailRec: Boolean
)

sealed class ClassStatement {
    data class Constructor(val params: List<FunParam>, val primaryCall: CallArgs, val body: PExpr) : ClassStatement()
    data class Initializer(val scope: ExprScope) : ClassStatement()
    data class Method(val function: Function, val info: MethodInfo) : ClassStatement()
    data class VProperty(val property: Property<ClassLevelPrivacy>, val info: ClassPropertyInfo) : ClassStatement()
    data class InnerClass(val clazz: Class) : ClassStatement()
    data class Extensions(val extension: Extension<ClassLevelPrivacy>) : ClassStatement()
}