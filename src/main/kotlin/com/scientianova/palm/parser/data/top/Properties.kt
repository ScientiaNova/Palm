package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.util.PString

sealed class BackedProperty {
    data class Normal(
        val name: PString,
        val modifiers: List<DecModifier>,
        val mutable: Boolean,
        val type: PType?,
        val expr: PExpr?,
        val getterModifiers: List<DecModifier>,
        val getter: Getter?,
        val setterModifiers: List<DecModifier>,
        val setter: Setter?
    ) : BackedProperty()

    data class Delegated(
        val name: PString,
        val modifiers: List<DecModifier>,
        val typeParams: List<PString>,
        val mutable: Boolean,
        val type: PType?,
        val delegate: PExpr
    ) : BackedProperty()
}

data class Getter(val type: PType?, val expr: PExpr?)
data class Setter(val type: PType?, val param: OptionallyTypedFunParam, val expr: PExpr)

data class Property(
    val name: PString,
    val modifiers: List<DecModifier>,
    val typeParams: List<PString>,
    val mutable: Boolean,
    val type: PType?,
    val getterModifiers: List<DecModifier>,
    val getter: Getter?,
    val setterModifiers: List<DecModifier>,
    val setter: Setter?
)