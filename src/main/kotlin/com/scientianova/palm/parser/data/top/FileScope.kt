package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.expressions.Path
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.data.types.Enum
import com.scientianova.palm.util.PString

data class FileScope(val path: Path?, val imports: List<Import>, val declarations: List<FileStatement>)

sealed class FileStatement

data class StaticFunction(val function: Function) : FileStatement()
data class StaticProperty(val property: BackedProperty) : FileStatement()
data class StaticClass(val clazz: Class) : FileStatement()
data class StaticRecord(val record: Record) : FileStatement()
data class StaticEnum(val enum: Enum) : FileStatement()
data class StaticObject(val obj: Object) : FileStatement()
data class StaticExtensions(val extension: Extension) : FileStatement()
data class StaticImpl(val implementation: Implementation) : FileStatement()
data class StaticTrait(val trait: Trait) : FileStatement()
data class StaticMixin(val mixin: Mixin) : FileStatement()

data class TypeAlias(
    val name: PString,
    val modifiers: List<DecModifier>,
    val params: List<PString>,
    val actual: PType,
) : FileStatement()

data class Constant(
    val name: PString,
    val modifiers: List<DecModifier>,
    val type: PType?,
    val expr: PExpr
) : FileStatement()