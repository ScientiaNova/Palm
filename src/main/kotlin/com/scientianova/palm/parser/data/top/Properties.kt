package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.TypeParam
import com.scientianova.palm.util.PString

sealed class Property<I> {
    data class Normal<I>(
        val name: PString,
        val typeParams: List<TypeParam>,
        val mutable: Boolean,
        val type: PType?,
        val expr: PExpr?,
        val getter: Getter<I>?,
        val setter: Setter<I>?
    ) : Property<I>()

    data class Delegated<I>(
        val name: PString,
        val typeParams: List<TypeParam>,
        val mutable: Boolean,
        val type: PType?,
        val delegate: PExpr
    ) : Property<I>()
}

data class Getter<I>(val info: I, val type: PType?, val expr: PExpr?)
data class Setter<I>(val info: I, val type: PType?, val paramName: PString, val paramType: PType?, val expr: PExpr)