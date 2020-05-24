package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Declaration : IStatement
typealias PDeclaration = Positioned<Declaration>

data class VarDec(
    val name: PString,
    val type: PType,
    val mutable: Boolean
) : Declaration()

data class VarDecAndAssign(
    val pattern: PDecPattern,
    val expr: PExpression?,
    val mutable: Boolean
) : Declaration()

data class FunctionDec(
    val name: PString,
    val params: List<Pair<PDecPattern, PType?>>,
    val typeReqs: List<PTypeReq>,
    val returnType: PType?,
    val expr: PExpression
) : Declaration()

data class RecordParam(
    val name: PString,
    val type: PType
)

data class RecordDeclaration(
    val name: PString,
    val params: List<RecordParam>,
    val genericPool: List<PString>
) : Declaration()

data class EnumCase(
    val name: PString,
    val args: List<PType>,
    val returnType: PType,
    val typeReqs: List<PTypeReq>
)

data class EnumDec(
    val name: PString,
    val genericPool: List<PString>,
    val implementations: List<PType>,
    val cases: List<EnumCase>
) : Declaration()

data class AliasDec(
    val name: PString,
    val genericPool: List<PString>,
    val actualType: PType
) : Declaration()

data class PrecedenceGroupDec(
    val before: PathExpr,
    val after: PathExpr,
    val leftAssociative: Boolean
) : Declaration()

data class OperatorDec(
    val symbol: PString,
    val groupPath: PathExpr,
    val name: PString
) : Declaration()