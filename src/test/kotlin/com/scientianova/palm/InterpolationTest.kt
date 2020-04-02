package com.scientianova.palm

import com.scientianova.palm.registry.parseExpression

fun main() {
    println("\"\${x[1]}\" where { x = [1 + 2, -5, 10] }".parseExpression().evaluate())
}