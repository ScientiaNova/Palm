package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.util.PString

sealed class Implementation {
    data class Normal(
        val typeParams: PString,
        val on: PType,
        val typeConstraints: TypeConstraints,
        val statements: List<ImplStatement>
    )

    data class Trait(
        val trait: PType,
        val typeParams: PString,
        val on: PType,
        val typeConstraints: TypeConstraints,
        val statements: List<TraitImplStatement>
    )
}

sealed class ImplStatement {
    data class Method(val function: Function) : ImplStatement()
    data class VProperty(val property: Property) : ImplStatement()
}

sealed class TraitImplStatement {
    data class Method(val function: Function) : TraitImplStatement()
    data class VProperty(val property: Property) : TraitImplStatement()
    data class AssociatedType(val name: PString, val type: PType) : TraitImplStatement()
}