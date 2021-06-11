package com.palmlang.palm.errors

import com.palmlang.palm.util.StringPos
import java.net.URL

data class PalmError(val msg: String, val file: URL, val start: StringPos, val next: StringPos)