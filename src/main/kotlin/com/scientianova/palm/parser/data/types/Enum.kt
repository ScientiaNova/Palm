package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

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

    data class Normal(
        val name: PString,
        val annotations: List<Annotation>,
        val components: List<CaseProperty>
    ) : EnumCase()

    data class Single(val name: PString, val annotations: List<Annotation>) : EnumCase()
}

data class CaseProperty(val name: PString, val annotations: List<Annotation>, val type: PType)