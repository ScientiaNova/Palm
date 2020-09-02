package com.scientianova.palm.parser

import com.scientianova.palm.util.PString

sealed class Import

data class RegularImport(val path: List<PString>, val alias: PString) : Import()
data class PackageImport(val path: List<PString>) : Import()