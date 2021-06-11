package com.palmlang.palm.ast.top

import com.palmlang.palm.ast.expressions.PExpr
import com.palmlang.palm.ast.expressions.PType

data class Getter(val type: PType?, val expr: PExpr?)
data class Setter(val param: OptionallyTypedFunParam, val type: PType?, val expr: PExpr)