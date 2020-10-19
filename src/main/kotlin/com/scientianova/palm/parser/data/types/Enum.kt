package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString

data class Enum(
    val name: PString,
    val modifiers: List<DecModifier>,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val cases: List<EnumCase>
)

sealed class EnumCase {
    data class Tuple(
        val name: PString,
        val annotations: List<Annotation>,
        val components: List<PType>
    ) : EnumCase()

    data class Single(val name: PString, val annotations: List<Annotation>) : EnumCase()
}