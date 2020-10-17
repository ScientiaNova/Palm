package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

data class Enum(
    val name: PString,
    val modifier: List<DecModifier>,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val cases: List<Positioned<EnumCase>>
)

sealed class EnumCase {
    data class Tuple(
        val name: PString,
        val modifiers: List<DecModifier>,
        val components: List<PType>
    ) : EnumCase()

    data class Normal(
        val name: PString,
        val modifiers: List<DecModifier>,
        val components: List<RecordProperty>
    ) : EnumCase()

    data class Single(val modifiers: List<DecModifier>, val name: PString) : EnumCase()
}