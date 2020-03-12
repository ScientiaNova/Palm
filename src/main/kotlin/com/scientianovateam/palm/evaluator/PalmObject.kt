package com.scientianovateam.palm.evaluator

import com.scientianovateam.palm.parser.NULL_TYPE
import com.scientianovateam.palm.parser.PalmType

data class PalmObject(val obj: Any?, val type: PalmType)

val NULL_OBJECT = PalmObject(null, NULL_TYPE)