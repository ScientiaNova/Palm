package com.scientianova.palm.util

typealias LineEnds = List<Int>

typealias Row = Int
typealias Column = Int
typealias StringCoord = Pair<Row, Column>

val StringCoord.row get() = first
val StringCoord.column get() = second

private tailrec fun findLineEnds(string: String, index: Int, soFar: LineEnds): LineEnds =
    if (index < string.length)
        if (string[index] == '\n')
            findLineEnds(string, index + 1, soFar + index)
        else findLineEnds(string, index + 1, soFar)
    else soFar + index

val String.lineEnds get() = findLineEnds(this, 0, emptyList())

tailrec fun LineEnds.coordFor(pos: StringPos, rowIndex: Int = 0): StringCoord =
    if (pos <= get(rowIndex)) StringCoord(rowIndex + 1, pos + 1 - (getOrNull(rowIndex - 1) ?: 0))
    else coordFor(pos, rowIndex + 1)

tailrec fun LineEnds.onSameRow(first: StringPos, second: StringPos, rowIndex: Int = 0): Boolean {
    val rowLast = get(rowIndex)
    return when {
        first <= rowLast -> second <= rowLast
        second <= rowLast -> false
        else -> onSameRow(first, second, rowIndex + 1)
    }
}