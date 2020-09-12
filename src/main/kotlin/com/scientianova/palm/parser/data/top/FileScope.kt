package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Path
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.data.types.Enum

enum class TopLevelPrivacy {
    Public, Internal, Private
}

data class FileScope(val path: Path?, val imports: List<Import>, val declarations: List<FileStatement>)

data class FilePropertyInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val lateInit: Boolean,
    val given: Boolean,
    val using: Boolean
)

data class FileFunctionInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val tailRec: Boolean,
    val given: Boolean,
    val using: Boolean
)

sealed class FileStatement

data class StaticFunction(val function: Function, val info: FileFunctionInfo) : FileStatement()
data class StaticProperty(val property: Property<TopLevelPrivacy>, val info: FilePropertyInfo) : FileStatement()
data class StaticAlias(val type: Alias) : FileStatement()
data class StaticClass(val clazz: Class) : FileStatement()
data class StaticRecord(val record: Record) : FileStatement()
data class StaticEnum(val enum: Enum) : FileStatement()
data class StaticObject(val obj: Object) : FileStatement()
data class StaticExtensions(val extension: Extension<TopLevelPrivacy>) : FileStatement()