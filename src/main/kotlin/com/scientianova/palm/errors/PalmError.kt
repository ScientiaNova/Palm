package com.scientianova.palm.errors

import com.scientianova.palm.util.StringPos
import java.net.URL

data class PalmError(val msg: String, val file: URL, val start: StringPos, val next: StringPos)