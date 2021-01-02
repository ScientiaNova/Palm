package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExprScope
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.Implementation
import com.scientianova.palm.parser.data.types.TypeClass
import com.scientianova.palm.parser.data.types.TypeDec
import com.scientianova.palm.util.PString

data class FileScope(
    val annotations: List<Annotation>,
    val imports: List<Import>,
    val statements: List<FileStmt>
)

sealed class FileStmt {
    data class Init(val scope: PExprScope) : FileStmt()
    data class Fun(val function: Function) : FileStmt()
    data class Prop(val property: Property) : FileStmt()
    data class Type(val dec: TypeDec) : FileStmt()
    data class TC(val tc: TypeClass) : FileStmt()
    data class Impl(val implementation: Implementation) : FileStmt()

    data class TypeAlias(
        val name: PString,
        val modifiers: List<DecModifier>,
        val params: List<PString>,
        val actual: PType,
    ) : FileStmt()
}