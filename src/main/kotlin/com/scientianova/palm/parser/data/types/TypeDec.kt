package com.scientianova.palm.parser.data.types

import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.util.PString

sealed class TypeDec {
    data class Class(
        val name: PString,
        val modifiers: List<DecModifier>,
        val constructorModifiers: List<DecModifier>,
        val primaryConstructor: List<PrimaryParam>?,
        val typeParams: List<PClassTypeParam>,
        val typeConstraints: TypeConstraints,
        val superTypes: List<PSuperType>,
        val statements: List<ClassStmt>
    ) : TypeDec()

    data class Object(
        val name: PString,
        val modifiers: List<DecModifier>,
        val superTypes: List<PSuperType>,
        val statements: List<ObjStmt>
    ) : TypeDec()

    data class Interface(
        val name: PString,
        val modifiers: List<DecModifier>,
        val typeParams: List<PClassTypeParam>,
        val typeConstraints: TypeConstraints,
        val superTypes: List<PType>,
        val statements: List<InterfaceStmt>
    ) : TypeDec()
}