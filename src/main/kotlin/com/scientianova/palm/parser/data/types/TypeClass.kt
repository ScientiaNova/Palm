package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString

sealed class TCStmt {
    data class Method(val function: Function) : TCStmt()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : TCStmt()
    data class AssociatedType(val name: PString, val bound: PType?, val default: PType?) : TCStmt()
    data class NestedDec(val dec: TypeDec) : TCStmt()
}

data class TypeClass(
    val name: PString,
    val modifiers: List<DecModifier>,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val superTypes: List<PType>,
    val statements: List<TCStmt>
)