package com.palmlang.palm.parser

import com.palmlang.palm.ast.top.ModuleAST
import com.palmlang.palm.parser.top.parseFile
import java.net.URL

fun testParseFile(path: URL): ModuleAST {
    val parser = SharedParseData(mutableListOf()).fileParser(path)
    val ast = parser.parseFile()
    parser.errors.forEach(::println)
    return ast
}
