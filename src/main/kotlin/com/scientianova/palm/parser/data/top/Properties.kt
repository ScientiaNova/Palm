package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.util.PString

sealed class Property {
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
    ) : Property()

    data class Delegated(
        val name: PString,
        val modifiers: List<DecModifier>,
        val mutable: Boolean,
        val type: PType?,
        val delegate: PExpr
    ) : Property()
}

data class Getter(val type: PType?, val expr: PExpr?)
data class Setter(val param: OptionallyTypedFunParam, val type: PType?, val expr: PExpr)