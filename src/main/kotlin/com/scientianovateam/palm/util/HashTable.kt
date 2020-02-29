package com.scientianovateam.palm.util

class HashTable<R, C, V> : HashMap<R, HashMap<C, V>>() {
    operator fun set(row: R, column: C, value: V) {
        val map = get(row) ?: hashMapOf<C, V>().also { put(row, it) }
        map[column] = value
    }

    operator fun get(row: R, column: C) = get(row)?.get(column)

    val rows get() = values
}