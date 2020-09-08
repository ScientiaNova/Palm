package com.scientianova.palm.parser.types

import com.scientianova.palm.util.PString

data class Alias(val name: PString, val params: List<PString>, val actual: PType)