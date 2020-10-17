package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.BackedProperty
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class SuperType {
    data class Class(val type: PType, val args: CallArgs, val mixins: List<PType>) : SuperType()
    data class Interface(val type: PType, val delegate: PString?) : SuperType()
}

data class SuperClass(val type: PType, val args: CallArgs, val mixins: List<PType>)

data class Class(
    val name: PString,
    val modifiers: List<DecModifier>,
    val constructorModifiers: List<DecModifier>,
    val primaryConstructor: List<PrimaryParam>,
    val typeParams: List<PClassTypeParam>,
    val typeConstraints: TypeConstraints,
    val superTypes: List<SuperType>,
    val statements: List<ClassStatement>
)

sealed class PrimaryParam {
    data class Normal(val param: FunParam) : PrimaryParam()
    data class Property(
        val name: PString,
        val modifiers: List<DecModifier>,
        val mutable: Boolean,
        val type: PType,
        val default: PExpr
    ) : PrimaryParam()
}

sealed class ClassStatement {
    data class Constructor(
        val modifiers: List<DecModifier>,
        val params: List<FunParam>,
        val primaryCall: CallArgs,
        val body: PExpr
    ) : ClassStatement()

    data class Initializer(val scope: ExprScope) : ClassStatement()
    data class Method(val function: Function) : ClassStatement()
    data class Property(val property: BackedProperty) : ClassStatement()
}

data class ClassTypeParam(val type: PString, val variance: VarianceMod)
typealias PClassTypeParam = Positioned<ClassTypeParam>

typealias TypeConstraints = List<Pair<PString, PType>>