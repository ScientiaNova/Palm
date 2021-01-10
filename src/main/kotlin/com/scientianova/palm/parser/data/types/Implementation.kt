package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.ContextParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.Item
import com.scientianova.palm.util.PString

data class Implementation(
    val type: PType,
    val typeParams: List<PString>,
    val typeConstraints: TypeConstraints,
    val context: List<ContextParam>,
    val items: List<Item>
)