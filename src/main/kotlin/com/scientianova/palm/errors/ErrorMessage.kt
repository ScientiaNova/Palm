package com.scientianova.palm.errors

import com.scientianova.palm.util.*

typealias PError = Positioned<PalmError>

fun PError.messageFor(code: String, fileName: String) =
    """

-- ${value.name} ${"-".repeat(80 - value.name.length - fileName.length)} $fileName

${value.context.wrap()}

${highlightError(code, start, next)}
${value.help.lines().map { it.wrap() }.joinToString("\n") { it }}

-------------------------------------------------------------------------------------

""".trimIndent()

private fun highlightError(code: String, start: StringPos, end: StringPos): String {
    val lineEnds = findLineEnds(code)
    val firstCoord = lineEnds.coordFor(start)
    val secondCoord = lineEnds.coordFor(end - 1)
    return if (firstCoord.row == secondCoord.row) {
        val row = firstCoord.row
        val rowStr = (row + 1).toString()
        val line = code.substring(lineEnds.getOrNull(row - 1)?.plus(1) ?: 0, lineEnds[row])
        """
$rowStr| $line
${" ".repeat(rowStr.length)}  ${line.take(firstCoord.column).replace("[^ \t]".toRegex(), " ")}${
            "^".repeat(end - start)
        }
""".trimIndent()
    } else {
        val codeLines = code.lines()
        val numberLength = secondCoord.row.toString().length
        (firstCoord.row..secondCoord.row).map {
            "$it${" ".repeat(it.toString().length - numberLength)}|>${codeLines[it - 1]}"
        }.joinToString("\n") { it } + '\n'
    }
}

fun String.wrap(perLine: Int = 85): String = if (length <= perLine) this else {
    val splitIndex = lastIndexOf(' ', perLine)
    take(splitIndex) + '\n' + drop(splitIndex + 1).wrap(perLine)
}