package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PExprScope
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.util.PString

sealed class ObjStmt {
    data class Initializer(val scope: PExprScope) : ObjStmt()
    data class Item(val item: com.scientianova.palm.parser.data.top.Item) : ObjStmt()
}

data class Object(
    val name: PString,
    val modifiers: List<PDecMod>,
    val superTypes: List<PSuperType>,
    val statements: List<ObjStmt>
)