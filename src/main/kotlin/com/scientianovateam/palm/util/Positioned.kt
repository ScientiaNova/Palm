package com.scientianovateam.palm.util

data class Positioned<out T>(val value: T, val rows: IntRange) {
    override fun toString() = value.toString()
}

infix fun <T> T.on(other: Positioned<*>) =
    Positioned(this, other.rows)
infix fun <T> T.on(row: Int) = Positioned(this, row..row)
infix fun <T> T.on(rows: IntRange) = Positioned(this, rows)