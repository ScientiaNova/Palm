package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.TypeParam
import com.scientianova.palm.util.PString

sealed class Property<P> {
    data class Normal<P>(
        val name: PString,
        val typeParams: List<TypeParam>,
        val mutable: Boolean,
        val type: PType?,
        val expr: PExpr?,
        val getter: Getter<P>?,
        val setter: Setter<P>?
    ) : Property<P>()

    data class Delegated<P>(
        val name: PString,
        val typeParams: List<TypeParam>,
        val mutable: Boolean,
        val type: PType?,
        val delegate: PExpr
    ) : Property<P>()
}

data class Getter<P>(val privacy: P, val type: PType?, val expr: PExpr)
data class Setter<P>(val privacy: P, val type: PType?, val paramName: PString, val paramType: PType?, val expr: PExpr)