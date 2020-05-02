package com.scientianova.palm.parser

interface IScope {
    val statements: MutableList<PStatement>
}

class SolidScope : IScope {
    override val statements = mutableListOf<PStatement>()
}

class NamedScope(val name: String = "") : IScope {
    override val statements = mutableListOf<PStatement>()
}