package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

data class Enum(
    val name: PString,
    val typeParams: List<TypeParam>,
    val typeConstraints: TypeConstraints,
    val cases: List<Positioned<EnumCase>>
)

sealed class EnumCase {
    data class Tuple(
        val name: PString,
        val components: List<PType>
    ) : EnumCase()

    data class Normal(
        val name: PString,
        val components: List<Pair<PString, PType>>
    ) : EnumCase()

    data class Single(val name: PString) : EnumCase()
}