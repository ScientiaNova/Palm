package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString

sealed class Implementation {
    data class Inherent(
        val type: PType,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
        val statements: List<ImplStatement>
    ) : Implementation()

    data class Trait(
        val trait: PType,
        val on: PType,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
        val statements: List<TraitImplStatement>
    ) : Implementation()
}

sealed class ImplStatement {
    data class Method(val function: Function) : ImplStatement()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : ImplStatement()
}

sealed class TraitImplStatement {
    data class Method(val function: Function) : TraitImplStatement()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : TraitImplStatement()
    data class AssociatedType(val name: PString, val type: PType) : TraitImplStatement()
}