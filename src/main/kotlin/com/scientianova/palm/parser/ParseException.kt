package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

data class ParseException(val error: PalmError) : Exception()

fun parseErr(error: String, start: StringPos, next: StringPos = start + 1): Nothing =
    throw ParseException(error.at(start, next))

fun parseErr(error: PalmError): Nothing =
    throw ParseException(error)