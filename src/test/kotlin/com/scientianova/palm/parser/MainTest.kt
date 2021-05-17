package com.scientianova.palm.parser

import com.scientianova.palm.util.Positioned
import java.net.URL
import kotlin.system.measureTimeMillis

fun resource(name: String): URL = Positioned::class.java.getResource("/test_code/$name")!!

fun main() {
    testParseFile(resource("aoc_example.palm"))
}

inline fun benchmark(times: Int, block: () -> Unit) {
    block()
    var sum = 0L
    for (i in 1..times) {
        sum += measureTimeMillis(block)
    }
    println(sum / times.toDouble())
}