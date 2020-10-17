package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.BackedProperty
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString

data class Object(
    val name: PString,
    val modifiers: List<DecModifier>,
    val superTypes: List<SuperType>,
    val statements: List<ObjectStatement>
)

sealed class ObjectStatement {
    data class Initializer(val scope: ExprScope) : ObjectStatement()
    data class Method(val function: Function) : ObjectStatement()
    data class Property(val property: BackedProperty) : ObjectStatement()
}