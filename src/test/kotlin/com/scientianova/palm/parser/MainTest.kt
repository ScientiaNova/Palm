package com.scientianova.palm.parser

import com.scientianova.palm.parser.parsing.requireBinOps
import kotlin.system.measureTimeMillis

fun main() {
    benchmark(100_000) {
        testParse(oldExample, ::requireBinOps)
    }
}

inline fun benchmark(times: Int, block: () -> Unit) {
    block()
    var sum = 0L
    for (i in 1..times) {
        sum += measureTimeMillis(block)
    }
    println(sum / times.toDouble())
}