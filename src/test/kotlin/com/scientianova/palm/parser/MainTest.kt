package com.scientianova.palm.parser

import kotlin.system.measureTimeMillis

fun main() {
    parserLargeFile()
}

inline fun benchmark(times: Int, block: () -> Unit) {
    var sum = 0L
    for (i in 1..times) {
        sum += measureTimeMillis(block)
    }
    println(sum / times.toDouble())
}