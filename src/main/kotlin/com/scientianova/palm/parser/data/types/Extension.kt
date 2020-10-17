package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString

data class Extension(
    val on: List<ExtensionType>,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val body: List<ExtensionStatement>
)

data class ExtensionType(val type: PType, val alias: PString?)

sealed class ExtensionStatement {
    data class Method(val function: Function) : ExtensionStatement()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : ExtensionStatement()
    data class Extension(val extension: com.scientianova.palm.parser.data.types.Extension) : ExtensionStatement()
}