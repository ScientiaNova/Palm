package com.scientianova.palm.parser

import com.scientianova.palm.util.Positioned

interface IStatement
typealias PStatement = Positioned<IStatement>

sealed class ImportStmt
data class RegularImport(val path: List<String>, val alias: String) : ImportStmt()
data class PackageImport(val path: List<String>) : ImportStmt()

data class Constructor(val params: List<Pair<String, PType>>, val scope: NamedScope)
data class Initializer(val scope: NamedScope)

data class Implementation(val genericPool: List<PTypeVar>, val forType: PType, val scope: NamedScope)