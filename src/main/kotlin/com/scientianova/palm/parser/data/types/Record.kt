package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.util.PString

sealed class Record {
    data class Tuple(
        val name: PString,
        val typeParams: List<TypeParam>,
        val typeConstraints: TypeConstraints,
        val components: List<PType>
    ) : Record()

    data class Normal(
        val name: PString,
        val typeParams: List<TypeParam>,
        val typeConstraints: TypeConstraints,
        val components: List<Pair<PString, PType>>
    ) : Record()

    data class Single(val name: PString) : Record()
}