package com.scientianova.palm

import com.scientianova.palm.registry.parseExpression

fun main() {
    println("[n for nested in list for n in nested] where { list = [2 4; 5 10] }".parseExpression().evaluate())
}