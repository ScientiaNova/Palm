package com.scientianova.palm

import com.scientianova.palm.parser.parse

fun main() = println(
    parse(
        """
{
 name = "Test ${'$'}name"
 expr = 2 * 2 ^ 2 / 2 - 2
 id = 124
 pattern = [p null null; p p null; p p p] where {
  p = tags["wooden_planks"]
 }
}
""".trimIndent()
    )
)