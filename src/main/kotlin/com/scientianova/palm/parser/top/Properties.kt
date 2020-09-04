package com.scientianova.palm.parser.top

import com.scientianova.palm.parser.expressions.PExpr
import com.scientianova.palm.parser.types.PType
import com.scientianova.palm.parser.types.ClassLevelPrivacy
import com.scientianova.palm.util.PString


sealed class Property<P> {
    data class Normal<P>(
        val name: PString,
        val mutable: Boolean,
        val type: PType?,
        val expr: PExpr?,
        val getter: Getter<P>,
        val setter: Setter<P>,
        val privacy: P
    ) : Property<P>()

    data class Delegated<P>(
        val name: PString,
        val mutable: Boolean,
        val type: PType?,
        val delegate: PExpr?,
        val privacy: P
    ) : Property<P>()
}

data class Getter<P>(val privacy: P, val expr: PExpr)
data class Setter<P>(val privacy: P, val paramName: PString, val paramType: PType?, val expr: PExpr)