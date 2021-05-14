package com.scientianova.palm.parser.data.top

data class ModuleScope(
    val annotations: List<Annotation>,
    val imports: List<Import>
)