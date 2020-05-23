package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmCompilationException
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.tokenizer.PToken
import com.scientianova.palm.tokenizer.TokenList
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

class Parser(private val tokens: TokenList, private val code: String, val fileName: String = "REPL") {
    fun pop(): PToken? = tokens.poll()

    val lastPos = code.lines().let {
        StringPos(it.size.coerceAtLeast(1), it.lastOrNull()?.run { length + 1 } ?: 1)
    }

    val lastArea get() = lastPos..lastPos

    fun handle(list: TokenList) = Parser(list, code, fileName)

    fun error(error: PalmError, area: StringArea): Nothing =
        throw PalmCompilationException(code, fileName, area, error)

    fun error(error: PalmError, pos: StringPos): Nothing =
        throw PalmCompilationException(code, fileName, pos..pos, error)
}

fun handleTopLevel(token: PToken, parser: Parser, scope: FileScope) {

}