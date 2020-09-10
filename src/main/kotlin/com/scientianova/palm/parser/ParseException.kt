package com.scientianova.palm.parser

import com.scientianova.palm.errors.PError
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

data class ParseException(val error: PError) : Exception()

fun parseErr(error: PalmError, start: StringPos, next: StringPos = start + 1): Nothing =
    throw ParseException(error.at(start, next))