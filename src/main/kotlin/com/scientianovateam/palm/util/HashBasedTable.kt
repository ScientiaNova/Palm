package com.scientianovateam.palm.util

class HashBasedTable<R, C, V> {
    private val maps = hashMapOf<R, HashMap<C, V>>()

    operator fun get(row: R) = maps[row]

    operator fun get(row: R, column: C) = maps[row]?.get(column)

    operator fun set(row: R, column: C, value: V) {
        val inner = maps[row] ?: hashMapOf<C, V>().also { maps[row] = it }
        inner[column] = value
    }

    val rows get() = maps.keys
    val columns = maps.flatMap { it.value.keys }
    val values = maps.flatMap { it.value.values }
}