package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Statement

data class ModuleAST(
    val annotations: List<Annotation>,
    val imports: List<Import>,
    val items: List<Statement>
)