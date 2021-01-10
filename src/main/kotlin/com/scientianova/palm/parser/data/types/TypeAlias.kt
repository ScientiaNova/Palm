package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.util.PString

data class TypeAlias(
    val name: PString,
    val modifiers: List<PDecMod>,
    val params: List<PString>,
    val bound: List<PType>,
    val actual: PType?
)