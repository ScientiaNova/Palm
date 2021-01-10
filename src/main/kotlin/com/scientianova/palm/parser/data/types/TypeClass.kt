package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Item
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.util.PString

data class TypeClass(
    val name: PString,
    val modifiers: List<PDecMod>,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val superTypes: List<PType>,
    val items: List<Item>
)