package com.scientianova.palm.parser

import kotlin.system.measureTimeMillis

fun main() {
    parserLargeFile()
}

inline fun benchmark(block: () -> Unit) {
    var sum = 0L
    for (i in 1..10000) {
        sum += measureTimeMillis(block)
    }
    println(sum / 10000.0)
}