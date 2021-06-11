package com.palmlang.palm.ast.top

import com.palmlang.palm.ast.expressions.Statement

data class ImplementationCase(
    val predicate: List<FunParam>,
    val items: List<Statement>
)

sealed class ImplementationKind {
    data class Single(val items: List<Statement>) : ImplementationKind()
    data class Cases(val cases: List<ImplementationCase>) : ImplementationKind()
}