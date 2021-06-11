package com.palmlang.palm.util

typealias LineEnds = List<Int>

typealias Row = Int
typealias Column = Int
typealias StringCoord = Pair<Row, Column>

val StringCoord.row get() = first
val StringCoord.column get() = second

fun findLineEnds(string: String): LineEnds {
    val indices = mutableListOf<Int>()
    var index = 0
    while (index < string.length) {
        if (string[index] == '\n') {
            indices.add(index)
        }
        index += 1
    }
    indices.add(index)

    return indices
}

tailrec fun LineEnds.coordFor(pos: StringPos, rowIndex: Int = 0): StringCoord =
    if (pos <= get(rowIndex)) {
        StringCoord(rowIndex, pos - (getOrNull(rowIndex - 1)?.plus(1) ?: 0))
    } else {
        coordFor(pos, rowIndex + 1)
    }

tailrec fun LineEnds.onSameRow(first: StringPos, second: StringPos, rowIndex: Int = 0): Boolean {
    val rowLast = get(rowIndex)
    return when {
        first <= rowLast -> second <= rowLast
        second <= rowLast -> false
        else -> onSameRow(first, second, rowIndex + 1)
    }
}