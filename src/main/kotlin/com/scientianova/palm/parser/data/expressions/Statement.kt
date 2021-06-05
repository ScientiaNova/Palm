package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Statement {
    data class Expr(val value: PExpr) : Statement()
    data class Defer(val body: PScope) : Statement()

    data class Var(
        val modifiers: List<PDecMod>,
        val pattern: PDecPattern,
        val type: PType?,
        val expr: PExpr?
    ) : Statement()

    data class Property(
        val name: PString,
        val modifiers: List<PDecMod>,
        val mutable: Boolean,
        val type: PType?,
        val context: List<FunParam>,
        val expr: PExpr?,
        val getterModifiers: List<PDecMod>,
        val getter: Getter?,
        val setterModifiers: List<PDecMod>,
        val setter: Setter?
    ) : Statement()

    data class Function(
        val local: Boolean,
        val name: PString,
        val modifiers: List<PDecMod>,
        val typeParams: List<PTypeParam>,
        val constraints: WhereClause,
        val context: List<FunParam>,
        val params: List<FunParam>,
        val type: PType?,
        val expr: PExpr?
    ) : Statement()

    data class Class(
        val name: PString,
        val modifiers: List<PDecMod>,
        val constructorModifiers: List<PDecMod>,
        val primaryConstructor: List<PrimaryParam>?,
        val typeParams: List<PTypeParam>,
        val typeConstraints: WhereClause,
        val superTypes: List<PSuperType>,
        val items: List<Statement>
    ) : Statement()

    data class Interface(
        val name: PString,
        val modifiers: List<PDecMod>,
        val typeParams: List<PTypeParam>,
        val typeConstraints: WhereClause,
        val superTypes: List<PType>,
        val items: List<Statement>
    ) : Statement()

    data class Object(
        val name: PString,
        val modifiers: List<PDecMod>,
        val superTypes: List<PSuperType>,
        val statements: List<Statement>
    ) : Statement()

    data class TypeClass(
        val name: PString,
        val modifiers: List<PDecMod>,
        val typeParams: List<PTypeParam>,
        val typeConstraints: WhereClause,
        val superTypes: List<PType>,
        val items: List<Statement>
    ) : Statement()

    data class Implementation(
        val type: PType,
        val typeParams: List<PTypeParam>,
        val typeConstraints: WhereClause,
        val context: List<FunParam>,
        val kind: ImplementationKind
    ) : Statement()

    data class TypeAlias(
        val name: PString,
        val modifiers: List<PDecMod>,
        val params: List<PString>,
        val bound: List<PType>,
        val actual: PType?
    ) : Statement()

    data class Constructor(
        val modifiers: List<PDecMod>,
        val params: List<FunParam>,
        val primaryCall: List<Arg<PExpr>>?,
        val body: PScope?
    ) : Statement()
}

typealias Scope = List<Statement>
typealias PScope = Positioned<Scope>