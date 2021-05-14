package com.scientianova.palm.parser

import com.scientianova.palm.parser.parsing.top.parseCrate
import com.scientianova.palm.parser.parsing.top.parseFile
import com.scientianova.palm.queries.ModuleId
import java.io.File
import java.net.URL

fun testParseFile(path: URL): ModuleId {
    return ModuleId().also {
        val parser = SharedParseData(mutableListOf()).fileParser(path)
        parser.parseFile(it)
        parser.errors.forEach(::println)
    }
}

fun testParseCrate(path: URL) {
    val parseData = parseCrate(File(path.toURI()))
    parseData.errors.forEach(::println)
}