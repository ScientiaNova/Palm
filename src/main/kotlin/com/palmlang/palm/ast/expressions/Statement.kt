package com.palmlang.palm.ast.expressions

import com.palmlang.palm.ast.ASTNode
import com.palmlang.palm.ast.top.*
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.Positioned

sealed class Statement : ASTNode {
    data class Expr(val value: PExpr) : Statement()

    data class Property(
        val pattern: PDecPattern,
        val modifiers: List<PDecMod>,
        val type: PType?,
        val context: List<FunParam>,
        val expr: PExpr?,
        val getterModifiers: List<PDecMod>,
        val getter: Getter?,
        val setterModifiers: List<PDecMod>,
        val setter: Setter?
    ) : Statement()

    data class Function(
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

data class Scope(val label: PString?, val header: LambdaHeader?, val statements: List<Statement>) : ASTNode
typealias PScope = Positioned<Scope>