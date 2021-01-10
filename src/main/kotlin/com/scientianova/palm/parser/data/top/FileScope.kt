package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExprScope

data class FileScope(
    val annotations: List<Annotation>,
    val imports: List<Import>,
    val statements: List<FileStmt>
)

sealed class FileStmt {
    data class Init(val scope: PExprScope) : FileStmt()
    data class Item(val item: com.scientianova.palm.parser.data.top.Item) : FileStmt()
}