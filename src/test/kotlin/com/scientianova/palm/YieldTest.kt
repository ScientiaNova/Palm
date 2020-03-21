package com.scientianova.palm

import com.scientianova.palm.registry.parseExpression

fun main() {
    println("(yield acc := acc + n for n in list) where { acc = 0 list = [1..100] }".parseExpression().evaluate())
}