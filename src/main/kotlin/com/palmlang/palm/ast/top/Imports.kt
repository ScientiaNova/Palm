package com.palmlang.palm.ast.top

import com.palmlang.palm.util.Path
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.PathType

data class Import(val pathType: PathType, val body: ImportBody)

sealed class ImportBody {
    abstract val path: Path

    data class File(override val path: Path) : ImportBody()
    data class Qualified(override val path: Path, val alias: PString) : ImportBody()
    data class Show(override val path: Path, val items: List<Pair<PString, PString?>>) : ImportBody()
    data class Hide(override val path: Path, val items: List<PString>) : ImportBody()
    data class Group(override val path: Path, val members: List<ImportBody>) : ImportBody()
}