package com.scientianova.palm.parser

import com.scientianova.palm.util.map

interface IScope {
    fun addStatement(statement: PStatement)
}

class FileScope : IScope {
    private val statements = mutableListOf<PStatement>()
    private val declaration = mutableListOf<PDeclaration>()
    override fun addStatement(statement: PStatement) {
        statements += statement
        if (statement.value is Declaration)
            declaration += statement.map { it as Declaration }
    }
}