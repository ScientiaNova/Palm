package com.palmlang.palm.parser.top

import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.top.ModuleAST

fun Parser.parseFile(): ModuleAST {
    val annotations = parseFileAnnotations()
    val imports = parseImports()
    val items = parseStatements()
    return ModuleAST(annotations, imports, items)
}