package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.PalmCompilationException
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

class StringTraverser(private val code: String, private val fileName: String) {
    private var index = 0
    fun peek() = code.getOrNull(index)
    fun pop() = code.getOrNull(index++).also {
        if (shouldUpdate) {
            lastRow++
            lastRowLastIndex = index
            shouldUpdate = false
        }
        if (it == '\n') shouldUpdate = true
    }

    private var shouldUpdate = false
    private var lastRowLastIndex = -1
    private var lastRow = 1
    private val lastColumn get() = index - lastRowLastIndex
    val lastPos get() = StringPos(lastRow, lastColumn)

    fun error(error: PalmError, area: StringArea): Nothing =
        throw PalmCompilationException(code, fileName, area, error)

    fun error(error: PalmError, pos: StringPos): Nothing =
        throw PalmCompilationException(code, fileName, pos..pos, error)
}