package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.expressions.VarianceMod
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

data class TypeParam(val name: PString, val params: List<PKindParam>)

data class ClassTypeParam(val type: TypeParam, val variance: VarianceMod)
typealias PClassTypeParam = Positioned<ClassTypeParam>

data class KindParam(val params: List<PKindParam>)
typealias PKindParam = Positioned<KindParam>

typealias TypeConstraints = List<Pair<PString, PType>>