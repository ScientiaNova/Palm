package com.palmlang.palm.ast.top

import com.palmlang.palm.util.Path
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.PathType

data class Import(val pathType: PathType, val body: ImportBody)

sealed interface ImportBody {
    val path: Path

    data class Module(override val path: Path, val exclude: List<PString>) : ImportBody
    data class Item(override val path: Path, val alias: PString?) : ImportBody
    data class Group(override val path: Path, val members: List<ImportBody>) : ImportBody
}