package com.palmlang.palm.ast.top

import com.palmlang.palm.ast.ASTNode
import com.palmlang.palm.ast.expressions.Statement

data class ModuleAST(
    val annotations: List<Annotation>,
    val imports: List<Import>,
    val items: List<Statement>
) : ASTNode