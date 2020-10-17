package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

enum class DecHandling {
    None, Val, Var
}

sealed class SuperType {
    data class Class(val type: PType, val args: List<Arg>, val mixins: List<PType>) : SuperType()
    data class Interface(val type: PType, val delegate: PString?) : SuperType()
}

data class SuperClass(val type: PType, val args: CallArgs, val mixins: List<PType>)

data class Class(
    val name: PString,
    val modifiers: List<DecModifier>,
    val constructorModifiers: List<DecModifier>,
    val primaryConstructor: List<PrimaryParam>?,
    val typeParams: List<PClassTypeParam>,
    val typeConstraints: TypeConstraints,
    val superTypes: List<SuperType>,
    val statements: List<ClassStatement>
)

data class PrimaryParam(
    val modifiers: List<DecModifier>,
    val decHandling: DecHandling,
    val name: PString,
    val type: PType,
    val default: PExpr?
)

sealed class ClassStatement {
    data class Constructor(
        val modifiers: List<DecModifier>,
        val params: List<FunParam>,
        val primaryCall: List<Arg>?,
        val body: ExprScope
    ) : ClassStatement()

    data class Initializer(val scope: ExprScope) : ClassStatement()
    data class Method(val function: Function) : ClassStatement()
    data class Property(val property: com.scientianova.palm.parser.data.top.Property) : ClassStatement()
}

data class ClassTypeParam(val type: PString, val variance: VarianceMod)
typealias PClassTypeParam = Positioned<ClassTypeParam>

typealias TypeConstraints = List<Pair<PString, PType>>