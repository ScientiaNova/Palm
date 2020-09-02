package com.scientianova.palm.errors

import com.scientianova.palm.util.*

data class PError(val error: PalmError, val area: StringArea)
infix fun PalmError.at(pos :StringPos) = PError(this, pos..pos)
infix fun PalmError.at(area: StringArea) = PError(this, area)

fun PError.messageFor(code: String, fileName: String) =
    """

-- ${error.name} ${"-".repeat(80 - error.name.length - fileName.length)} $fileName

${error.context.wrap()}

${highlightError(code, area.first, area.last)}
${error.help.lines().map { it.wrap() }.joinToString("\n") { it }}

-------------------------------------------------------------------------------------

""".trimIndent()

private fun highlightError(code: String, start: StringPos, end: StringPos): String {
    val lineEnds = code.lineEnds
    val firstCoord = lineEnds.coordFor(start)
    val secondCoord = lineEnds.coordFor(end)
    return if (firstCoord.row == secondCoord.row) {
        val row = firstCoord.row
        val line = code.slice((lineEnds.getOrNull(row - 2)?.plus(1) ?: 0) until lineEnds[row - 1])
        """
$row| $line
${" ".repeat(row.toString().length)}  ${line.take(firstCoord.column - 1).replace("[^ \t]".toRegex(), " ")}${
            "^".repeat(start - end + 1)
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