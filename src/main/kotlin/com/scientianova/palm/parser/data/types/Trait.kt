package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.util.PString

data class Trait(
    val name: PString,
    val typeParams: PString,
    val typeConstraints: TypeConstraints,
    val statements: List<TraitStatement>
)

sealed class TraitStatement {
    data class Method(val function: Function) : TraitStatement()
    data class VProperty(val property: Property) : TraitStatement()
    data class AssociatedType(val name: PString, val constraints: PType, val default: PType?) : TraitStatement()
}