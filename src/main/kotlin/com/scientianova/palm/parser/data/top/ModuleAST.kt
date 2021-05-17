package com.scientianova.palm.parser.data.top

data class ModuleAST(
    val annotations: List<Annotation>,
    val imports: List<Import>,
    val items: List<ItemKind>
)