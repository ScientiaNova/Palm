package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property

data class Extension<P>(
    val typeParams: TypeParam,
    val on: PType,
    val typeConstraints: TypeConstraints,
    val statements: List<ExtensionStatement<P>>
)

data class ExtensionPropertyInfo<P>(
    val privacy: P,
    val inline: Boolean
)

data class ExtensionMethodInfo<P>(
    val privacy: P,
    val operator: Boolean,
    val blank: Boolean,
    val tailRec: Boolean,
    val inline: Boolean
)

sealed class ExtensionStatement<P> {
    data class Method<P>(val function: Function, val info: ExtensionMethodInfo<P>) : ExtensionStatement<P>()
    data class VProperty<P>(val property: Property<P>, val info: ExtensionPropertyInfo<P>) : ExtensionStatement<P>()
    data class Extensions<P>(val extension: Extension<P>) : ExtensionStatement<P>()
}