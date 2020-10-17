package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString

data class Trait(
    val name: PString,
    val modifiers: List<DecModifier>,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val statements: List<TraitStatement>
)

sealed class TraitStatement {
    data class Method(val function: Function) : TraitStatement()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : TraitStatement()
    data class AssociatedType(val name: PString, val bound: PType?, val default: PType?) : TraitStatement()
}