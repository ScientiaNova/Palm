package com.scientianova.palm.parser

import com.scientianova.palm.tokenizer.DefinitionModifier
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

interface IDeclaration : IStatement
typealias PDeclaration = Positioned<IDeclaration>

sealed class Getter
data class MultiGetter(val scope: NamedScope) : Getter()
data class SingleGetter(val expr: PExpression) : Getter()

data class Setter(val name: String, val scope: NamedScope)

data class PropertyDec(
    val name: PString,
    val type: PType,
    val getter: Getter?,
    val setter: Setter?,
    val modifiers: List<DefinitionModifier>
) : IDeclaration

data class PropertyDecAndAssign(
    val pattern: PPattern,
    val expr: PExpression,
    val getter: Getter?,
    val setter: Setter?
) : IDeclaration

data class VarDec(
    val name: PString,
    val type: PType,
    val mutable: Boolean
) : IDeclaration

data class VarDecAndAssign(
    val pattern: PPattern,
    val expr: PExpression?
) : IDeclaration

data class SingleFunctionDec(
    val name: PString,
    val params: List<Pair<PString, PType>>,
    val genericPool: List<PTypeVar>,
    val returnType: Type?,
    val expr: PExpression,
    val modifiers: List<DefinitionModifier>
) : IDeclaration

data class MultiFunctionDec(
    val name: PString,
    val params: List<Pair<PString, PType>>,
    val genericPool: List<PTypeVar>,
    val returnType: PType,
    val scope: NamedScope,
    val modifiers: List<DefinitionModifier>
) : IDeclaration

sealed class SuperType
data class SuperInterface(val type: PType) : SuperType()
data class SuperClass(val type: PType, val params: List<PExpression>) : SuperType()


data class ObjectDec(
    val name: PString,
    val superTypes: List<SuperType>,
    val scope: NamedScope,
    val modifiers: List<DefinitionModifier>
) : IDeclaration

sealed class ClassParam

data class RegularClassParam(
    val name: PString,
    val type: PType
) : ClassParam()

data class DeclarationClassParam(
    val name: PString,
    val type: PType,
    val modifiers: List<DefinitionModifier>
) : ClassParam()

data class ClassDec(
    val name: PString,
    val params: List<ClassParam>,
    val genericPool: List<PTypeVar>,
    val superTypes: List<SuperType>,
    val scope: NamedScope,
    val modifiers: List<DefinitionModifier>
) : IDeclaration

data class RecordParam(
    val name: PString,
    val type: PType,
    val modifiers: List<DefinitionModifier>
)

data class RecordDeclaration(
    val name: PString,
    val params: List<RecordParam>,
    val genericPool: List<PTypeVar>,
    val superTypes: List<SuperType>,
    val scope: NamedScope,
    val modifiers: List<DefinitionModifier>
)

data class EnumCase(val name: PString, val args: List<PExpression>)

data class EnumDec(
    val name: PString,
    val params: List<ClassParam>,
    val genericPool: List<PTypeVar>,
    val implementations: List<PType>,
    val scope: NamedScope,
    val modifiers: List<DefinitionModifier>,
    val cases: List<EnumCase>
) : IDeclaration

data class AliasDec(
    val name: PString,
    val genericPool: List<PTypeVar>,
    val actualType: PType
) : IDeclaration

data class PrecedenceGroupDec(
    val before: List<PString>,
    val after: List<PString>,
    val assignment: Boolean
)

data class OperatorDec(
    val symbol: PString,
    val groupPath: List<PString>,
    val name: PString
)