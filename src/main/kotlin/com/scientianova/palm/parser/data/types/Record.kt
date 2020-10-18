package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString

sealed class Record {
    data class Tuple(
        val name: PString,
        val modifiers: List<DecModifier>,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
        val components: List<PType>
    ) : Record()

    data class Normal(
        val name: PString,
        val modifiers: List<DecModifier>,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
        val components: List<RecordProperty>
    ) : Record()

    data class Single(
        val name: PString,
        val modifiers: List<DecModifier>,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
    ) : Record()
}

data class RecordProperty(val name: PString, val modifiers: List<DecModifier>, val type: PType, val default: PExpr?)