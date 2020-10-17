package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Path
import com.scientianova.palm.util.PString

sealed class Import {
    data class Regular(val path: Path, val alias: PString?) : Import()
    data class Package(val path: Path) : Import()
    data class Group(val start: Path, val members: List<Import>) : Import()
}