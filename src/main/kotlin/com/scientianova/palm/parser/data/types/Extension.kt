package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.util.PString

data class Extension(
    val typeParams: PString,
    val on: List<ExtensionType>,
    val typeConstraints: TypeConstraints,
    val statements: List<ExtensionStatement>
)

data class ExtensionType(val type: PType, val alias: PString?)

sealed class ExtensionStatement {
    data class Method(val function: Function) : ExtensionStatement()
    data class VProperty(val property: Property) : ExtensionStatement()
    data class Extensions(val extension: Extension) : ExtensionStatement()
}