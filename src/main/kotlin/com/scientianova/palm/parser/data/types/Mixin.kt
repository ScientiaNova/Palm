package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

data class Mixin(
    val name: PString,
    val modifiers: List<DecModifier>,
    val typeParams: List<PClassTypeParam>,
    val typeConstraints: TypeConstraints,
    val on: List<PType>,
    val statements: List<MixinStatement>
)

sealed class MixinStatement {
    data class Method(val function: Function) : MixinStatement()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : MixinStatement()
}