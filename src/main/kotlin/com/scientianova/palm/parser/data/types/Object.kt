package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString

sealed class ObjStmt {
    data class Initializer(val scope: ExprScope) : ObjStmt()
    data class Method(val function: Function) : ObjStmt()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : ObjStmt()
    data class NestedDec(val dec: TypeDec) : ObjStmt()
}