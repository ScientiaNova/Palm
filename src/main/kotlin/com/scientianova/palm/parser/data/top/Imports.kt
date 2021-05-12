package com.scientianova.palm.parser.data.top

import com.scientianova.palm.util.Path
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.PathType

data class Import(val pathType: PathType, val body: ImportBody)

sealed class ImportBody {
    data class File(val path: Path) : ImportBody()
    data class Qualified(val path: Path, val alias: PString) : ImportBody()
    data class Show(val path: Path, val items: List<Pair<PString, PString?>>) : ImportBody()
    data class Hide(val path: Path, val items: List<PString>) : ImportBody()
    data class Group(val start: Path, val members: List<ImportBody>) : ImportBody()
}