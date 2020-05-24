package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

interface IStatement
typealias PStatement = Positioned<IStatement>

sealed class ImportStmt
data class RegularImport(val path: PathExpr, val alias: PString) : ImportStmt()
data class PackageImport(val path: PathExpr) : ImportStmt()

