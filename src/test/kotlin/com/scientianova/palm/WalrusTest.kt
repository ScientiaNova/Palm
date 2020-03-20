package com.scientianova.palm

import com.scientianova.palm.registry.parseExpression

fun main() {
    println(
        """
        when (rarity := 10 * 6) {
          0 -> "Common"
          in [1..50] -> "Uncommon (${'$'}{rarity}%)"
          in [51..99] -> "Rare (${'$'}{rarity}%)"
          else -> "Legendary"
        }
    """.trimIndent().parseExpression().evaluate()
    )
}