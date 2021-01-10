package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

enum class DecHandling {
    None, Val, Var
}

sealed class SuperType {
    data class Class(val type: PType, val args: List<Arg<PExpr>>) : SuperType()
    data class Interface(val type: PType, val delegate: PString?) : SuperType()
}

typealias PSuperType = Positioned<SuperType>

data class PrimaryParam(
    val modifiers: List<PDecMod>,
    val decHandling: DecHandling,
    val name: PString,
    val type: PType,
    val default: PExpr?
)

sealed class ClassStmt {
    data class Constructor(
        val modifiers: List<PDecMod>,
        val params: List<FunParam>,
        val primaryCall: List<Arg<PExpr>>?,
        val body: PExprScope?
    ) : ClassStmt()

    data class Initializer(val scope: PExprScope) : ClassStmt()
    data class Item(val item: com.scientianova.palm.parser.data.top.Item) : ClassStmt()
}

data class ClassTypeParam(val type: PString, val variance: VarianceMod)
typealias PClassTypeParam = Positioned<ClassTypeParam>

typealias TypeConstraints = List<Pair<PString, List<PType>>>

data class Class(
    val name: PString,
    val modifiers: List<PDecMod>,
    val constructorModifiers: List<PDecMod>,
    val primaryConstructor: List<PrimaryParam>?,
    val typeParams: List<PClassTypeParam>,
    val typeConstraints: TypeConstraints,
    val superTypes: List<PSuperType>,
    val statements: List<ClassStmt>
)