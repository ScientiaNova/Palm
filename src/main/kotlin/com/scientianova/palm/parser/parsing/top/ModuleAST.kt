package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ModuleAST
import com.scientianova.palm.parser.parsing.expressions.parseStatements

fun Parser.parseFile(): ModuleAST {
    val annotations = parseFileAnnotations()
    val imports = parseImports()
    val items = parseStatements()
    return ModuleAST(annotations, imports, items)
}