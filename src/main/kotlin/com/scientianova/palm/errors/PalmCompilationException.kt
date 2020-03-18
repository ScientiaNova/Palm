package com.scientianova.palm.errors

import com.scientianova.palm.util.StringArea

class PalmCompilationException(
    code: String,
    file: String,
    erroredArea: StringArea,
    error: PalmError
) : Exception(
    """

-- ${error.name} ${"-".repeat(80 - error.name.length - file.length)} $file

${error.context.wrap()}

${highlightError(code.lines(), erroredArea)}
${error.help.lines().map { it.wrap() }.joinToString("\n") { it }}

-------------------------------------------------------------------------------------
""".trimIndent()
)

private fun highlightError(codeLines: List<String>, posRange: StringArea) = posRange.run {
    if (start.row == end.row) {
        val row = start.row
        val line = codeLines[row - 1]
        """
$row| $line
${" ".repeat(row.toString().length)}  ${line.take(start.column - 1).replace("[^ \t]".toRegex(), " ")}${
        "^".repeat(end.column - start.column + 1)}
""".trimIndent()
    } else {
        val numberLength = end.row.toString().length
        (start.row..end.row).map { "$it${" ".repeat(it.toString().length - numberLength)}|>${codeLines[it]}" }
            .joinToString("\n") { it } + '\n'
    }
}

fun String.wrap(perLine: Int = 85): String =
    if (length <= perLine) this
    else {
        val splitIndex = lastIndexOf(' ', perLine)
        take(splitIndex) + '\n' + drop(splitIndex + 1).wrap(perLine)
    }