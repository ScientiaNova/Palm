package com.scientianova.palm.parser

import com.scientianova.palm.parser.data.top.ModuleAST
import com.scientianova.palm.parser.parsing.top.parseFile
import java.net.URL

fun testParseFile(path: URL): ModuleAST {
    val parser = SharedParseData(mutableListOf()).fileParser(path)
    val ast = parser.parseFile()
    parser.errors.forEach(::println)
    return ast
}
