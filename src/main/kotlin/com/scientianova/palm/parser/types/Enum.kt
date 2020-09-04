package com.scientianova.palm.parser.types

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

data class Enum(val name: PString, val typeParams: List<PTypeParam>, val cases: List<Positioned<Record>>)