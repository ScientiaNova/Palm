package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.top.Function

sealed class InterfaceStmt {
    data class Method(val function: Function) : InterfaceStmt()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : InterfaceStmt()
    data class NestedDec(val dec: TypeDec) : InterfaceStmt()
}