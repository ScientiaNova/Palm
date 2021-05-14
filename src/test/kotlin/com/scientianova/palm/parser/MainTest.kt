package com.scientianova.palm.parser

import com.scientianova.palm.queries.moduleNames
import com.scientianova.palm.util.Positioned
import java.net.URL
import kotlin.system.measureTimeMillis

fun resource(name: String): URL = Positioned::class.java.getResource("/test_code/$name")!!

fun main() {
    testParseCrate(resource("crate"))
    moduleNames.values.forEach(::println)
}

inline fun benchmark(times: Int, block: () -> Unit) {
    block()
    var sum = 0L
    for (i in 1..times) {
        sum += measureTimeMillis(block)
    }
    println(sum / times.toDouble())
}