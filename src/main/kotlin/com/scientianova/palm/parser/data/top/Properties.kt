package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.util.PString

sealed class PropertyBody {
    data class Normal(
        val expr: PExpr?,
        val getterModifiers: List<PDecMod>,
        val getter: Getter?,
        val setterModifiers: List<PDecMod>,
        val setter: Setter?
    ) : PropertyBody()

    data class Delegate(val expr: PExpr) : PropertyBody()
}

data class Getter(val type: PType?, val expr: PExpr?)
data class Setter(val param: OptionallyTypedFunParam, val type: PType?, val expr: PExpr)