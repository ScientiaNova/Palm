package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.expressions.Path
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.data.types.Enum
import com.scientianova.palm.util.PString

enum class TopLevelPrivacy {
    Public, Internal, Private
}

data class FileScope(val path: Path?, val imports: List<Import>, val declarations: List<FileStatement>)

data class FilePropertyInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val lateInit: Boolean,
    val given: Boolean
)

data class FileFunctionInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val tailRec: Boolean,
    val given: Boolean
)

sealed class FileStatement

data class StaticFunction(
    val function: Function,
    val info: FileFunctionInfo
) : FileStatement()

data class StaticProperty(
    val property: Property<TopLevelPrivacy>,
    val info: FilePropertyInfo
) : FileStatement()

data class TypeAlias(
    val name: PString,
    val params: List<PString>,
    val actual: PType,
    val privacy: TopLevelPrivacy
) : FileStatement()

data class Constant(
    val name: PString,
    val type: PType?,
    val expr: PExpr,
    val privacy: TopLevelPrivacy
) : FileStatement()

data class StaticClass(
    val clazz: Class
) : FileStatement()

data class StaticRecord(
    val record: Record,
    val privacy: TopLevelPrivacy,
    val inline: Boolean
) : FileStatement()

data class StaticEnum(
    val enum: Enum,
    val privacy: TopLevelPrivacy
) : FileStatement()

data class StaticObject(
    val obj: Object,
    val privacy: TopLevelPrivacy
) : FileStatement()

data class StaticExtensions(
    val extension: Extension<TopLevelPrivacy>
) : FileStatement()