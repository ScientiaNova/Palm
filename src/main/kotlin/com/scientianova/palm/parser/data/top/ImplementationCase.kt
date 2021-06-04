package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Statement

data class ImplementationCase(
    val predicate: List<FunParam>,
    val items: List<Statement>
)

sealed class ImplementationKind {
    data class Single(val items: List<Statement>) : ImplementationKind()
    data class Cases(val cases: List<ImplementationCase>) : ImplementationKind()
}