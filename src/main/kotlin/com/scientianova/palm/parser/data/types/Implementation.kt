package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.ContextParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString

data class Implementation(
    val type: PType,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val context: List<ContextParam>,
    val statements: List<ImplStmt>
)

sealed class ImplStmt {
    data class Method(val function: Function) : ImplStmt()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : ImplStmt()
    data class AssociatedType(val name: PString, val type: PType) : ImplStmt()
}