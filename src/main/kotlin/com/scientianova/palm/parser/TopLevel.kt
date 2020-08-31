package com.scientianova.palm.parser

import com.scientianova.palm.errors.unknownParamModifierError
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at

enum class DecProperty {
    Using,
    Given,
    Inline,
    Leaf,
    Abstract,
    Public,
    Private,
    Protected,
    Internal,
    Const,
    LateInit,
    TailRec,
    Inner
}

fun handleDecModifier(ident: PString, nextState: ParseState) = when (ident.value) {
    "using" -> DecProperty.Using at ident.area succTo nextState
    "given" -> DecProperty.Given at ident.area succTo nextState
    "inline" -> DecProperty.Inline at ident.area succTo nextState
    "lead" -> DecProperty.Leaf at ident.area succTo nextState
    "abstract" -> DecProperty.Abstract at ident.area succTo nextState
    "public" -> DecProperty.Public at ident.area succTo nextState
    "private" -> DecProperty.Private at ident.area succTo nextState
    "protected" -> DecProperty.Protected at ident.area succTo nextState
    "internal" -> DecProperty.Internal at ident.area succTo nextState
    "const" -> DecProperty.Const at ident.area succTo nextState
    "lateinit" -> DecProperty.LateInit at ident.area succTo nextState
    "tailrec" -> DecProperty.TailRec at ident.area succTo nextState
    "inner" -> DecProperty.Inner at ident.area succTo nextState
    else -> unknownParamModifierError errAt ident.area
}

enum class TopLevelPrivacy {
    Public, Internal, Private
}