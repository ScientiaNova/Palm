package com.scientianova.palm.parser

import com.scientianova.palm.parser.parsing.expressions.requireBinOps
import kotlin.system.measureTimeMillis

fun main() {
    println(testParse(oldExample, ::requireBinOps).toCodeString(0))
}

inline fun benchmark(times: Int, block: () -> Unit) {
    block()
    var sum = 0L
    for (i in 1..times) {
        sum += measureTimeMillis(block)
    }
    println(sum / times.toDouble())
}