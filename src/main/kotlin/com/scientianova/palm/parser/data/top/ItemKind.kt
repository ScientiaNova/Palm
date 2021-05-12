package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PExprScope
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.util.PString

sealed class ItemKind {
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
    ) : ItemKind()

    data class Function(
        val name: PString,
        val modifiers: List<PDecMod>,
        val typeParams: List<PString>,
        val constraints: TypeConstraints,
        val context: List<FunParam>,
        val params: List<FunParam>,
        val type: PType?,
        val expr: PExpr?
    ) : ItemKind()

    data class Class(
        val name: PString,
        val modifiers: List<PDecMod>,
        val constructorModifiers: List<PDecMod>,
        val primaryConstructor: List<PrimaryParam>?,
        val typeParams: List<PClassTypeParam>,
        val typeConstraints: TypeConstraints,
        val superTypes: List<PSuperType>,
        val secondaryConstructor: List<Constructor>,
        val items: List<ItemId>
    ) : ItemKind()

    data class Interface(
        val name: PString,
        val modifiers: List<PDecMod>,
        val typeParams: List<PClassTypeParam>,
        val typeConstraints: TypeConstraints,
        val superTypes: List<PType>,
        val items: List<ItemId>
    ) : ItemKind()

    data class Object(
        val name: PString,
        val modifiers: List<PDecMod>,
        val superTypes: List<PSuperType>,
        val statements: List<ItemId>
    ) : ItemKind()

    data class TypeClass(
        val name: PString,
        val modifiers: List<PDecMod>,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
        val superTypes: List<PType>,
        val items: List<ItemId>
    ) : ItemKind()

    data class Implementation(
        val type: PType,
        val typeParams: List<PString>,
        val typeConstraints: TypeConstraints,
        val context: List<FunParam>,
        val items: List<ItemId>
    ) : ItemKind()

    data class TypeAlias(
        val name: PString,
        val modifiers: List<PDecMod>,
        val params: List<PString>,
        val bound: List<PType>,
        val actual: PType?
    ) : ItemKind()

    data class Initializer(val scope: PExprScope) : ItemKind()
}