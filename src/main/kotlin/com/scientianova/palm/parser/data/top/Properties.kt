package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.util.PString

data class Property(
    val name: PString,
    val modifiers: List<DecModifier>,
    val mutable: Boolean,
    val type: PType?,
    val context: List<ContextParam>,
    val body: PropertyBody
)

sealed class PropertyBody {
    data class Normal(
        val expr: PExpr?,
        val getterModifiers: List<DecModifier>,
        val getter: Getter?,
        val setterModifiers: List<DecModifier>,
        val setter: Setter?
    ) : PropertyBody()

    data class Delegate(val expr: PExpr) : PropertyBody()
}

data class Getter(val type: PType?, val expr: PExpr?)
data class Setter(val param: OptionallyTypedFunParam, val type: PType?, val expr: PExpr)