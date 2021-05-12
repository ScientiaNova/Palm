package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType

data class Getter(val type: PType?, val expr: PExpr?)
data class Setter(val param: OptionallyTypedFunParam, val type: PType?, val expr: PExpr)