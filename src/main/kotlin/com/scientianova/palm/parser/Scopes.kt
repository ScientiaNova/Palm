package com.scientianova.palm.parser

interface IScope {
    fun addStatement(statement: PStatement)
}

@Suppress("UNCHECKED_CAST")
class FileScope : IScope  {
    private val statements = mutableListOf<PStatement>()
    private val declaration = mutableListOf<PDeclaration>()
    override fun addStatement(statement: PStatement) {
        statements += statement
        if (statement.value is IDeclaration)
            declaration += statement as PDeclaration
    }
}

class NamedScope(val name: String = "") : IScope {
    private val statements = mutableListOf<PStatement>()
    override fun addStatement(statement: PStatement) {
        statements += statement
    }
}