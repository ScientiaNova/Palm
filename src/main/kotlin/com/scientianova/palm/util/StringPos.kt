package com.scientianova.palm.util

data class StringPos(val row: Int, val column: Int) {
    operator fun rangeTo(other: StringPos) = StringArea(this, other)
    fun shift(columns: Int = 0, rows: Int = 0) = StringPos(row + rows, column + columns)
}

data class StringArea(val start: StringPos, val end: StringPos)