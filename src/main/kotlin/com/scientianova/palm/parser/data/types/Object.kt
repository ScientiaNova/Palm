package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.util.PString

data class Object(
    val name: PString,
    val superClass: SuperClass?,
    val implements: List<PType>,
    val statements: List<ObjectStatement>
)

sealed class ObjectStatement {
    data class Initializer(val scope: ExprScope) : ObjectStatement()
    data class Method(val function: Function, val info: MethodInfo) : ObjectStatement()
    data class VProperty(val property: Property<ClassLevelPrivacy>, val info: ClassPropertyInfo) : ObjectStatement()
    data class Extensions(val extension: Extension<ClassLevelPrivacy>) : ObjectStatement()
}