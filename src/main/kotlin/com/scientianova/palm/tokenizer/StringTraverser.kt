package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.PalmCompilationException
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

class StringTraverser(private val code: String, private val fileName: String) {
    private var pos = 0

    val lastPos get() = pos - 1

    fun peek() = code.getOrNull(pos)
    fun pop() = code.getOrNull(pos++)

    val beforePopped get() = code.getOrNull(pos - 2)

    fun error(error: PalmError, area: StringArea): Nothing =
        throw PalmCompilationException(code, fileName, area, error)

    fun error(error: PalmError, pos: StringPos): Nothing =
        throw PalmCompilationException(code, fileName, pos..pos, error)
}