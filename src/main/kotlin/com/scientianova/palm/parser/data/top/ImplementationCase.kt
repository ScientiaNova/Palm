package com.scientianova.palm.parser.data.top

data class ImplementationCase(
    val predicate: List<FunParam>,
    val items: List<ItemKind>
)

sealed class ImplementationKind {
    data class Single(val items: List<ItemKind>) : ImplementationKind()
    data class Cases(val cases: List<ImplementationCase>) : ImplementationKind()
}