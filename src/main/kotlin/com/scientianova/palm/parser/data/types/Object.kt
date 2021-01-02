package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PExprScope
import com.scientianova.palm.parser.data.top.Function

sealed class ObjStmt {
    data class Initializer(val scope: PExprScope) : ObjStmt()
    data class Method(val function: Function) : ObjStmt()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : ObjStmt()
    data class NestedDec(val dec: TypeDec) : ObjStmt()
}