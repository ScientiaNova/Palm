package com.scientianova.palm.parser.data.top

import com.scientianova.palm.util.Path
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.PathType

data class Import(val pathType: PathType, val body: ImportBody)

sealed class ImportBody {
    abstract val path: Path

    data class File(override val path: Path) : ImportBody()
    data class Qualified(override val path: Path, val alias: PString) : ImportBody()
    data class Show(override val path: Path, val items: List<Pair<PString, PString?>>) : ImportBody()
    data class Hide(override val path: Path, val items: List<PString>) : ImportBody()
    data class Group(override val path: Path, val members: List<ImportBody>) : ImportBody()
}