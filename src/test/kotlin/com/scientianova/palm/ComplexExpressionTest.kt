package com.scientianova.palm

import com.scientianova.palm.registry.parseExpression

fun main() {
    println("[n for n in nested for nested in list] where { list = [2, 4; 5, 10] }".parseExpression().evaluate())
}