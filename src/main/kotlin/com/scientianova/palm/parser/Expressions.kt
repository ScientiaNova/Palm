package com.scientianova.palm.parser

import com.scientianova.palm.tokenizer.DefinitionModifier
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

interface IExpression : IStatement
typealias PExpression = Positioned<IExpression>

sealed class ScopeSpecification
object CurrentScope : ScopeSpecification()
data class LabeledScope(val name: PString) : ScopeSpecification()

data class IfExpr(
    val conditions: List<Condition>,
    val thenScope: NamedScope,
    val elseScope: NamedScope?
) : IExpression

data class GuardExpr(
    val conditions: List<Condition>,
    val elseScope: NamedScope
) : IExpression

data class WhenExpr(
    val branches: List<Pair<PExpression, NamedScope>>,
    val elseBranch: NamedScope?
) : IExpression

data class WhenSwitchExpr(
    val branches: List<Pair<List<PPattern>, NamedScope>>
) : IExpression

data class LoopExpr(
    val scope: NamedScope
) : IExpression

data class WhileExpr(
    val condition: PExpression,
    val whileScope: NamedScope,
    val noBreakScope: NamedScope?
) : IExpression

data class ForExpr(
    val declaration: PPattern,
    val iterableExpr: PExpression,
    val forScope: NamedScope,
    val noBreakScope: NamedScope?
) : IExpression

data class TryExpr(
    val tryScope: NamedScope,
    val catchBranches: List<Pair<PPattern, PExpression>>,
    val finallyScope: NamedScope?
) : IExpression

data class ThrowExpr(
    val expr: PExpression
) : IExpression

data class ReturnExpr(
    val expr: PExpression?,
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class BreakExpr(
    val expr: PExpression?,
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class ContinueExpr(
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class ContinueWhenExpr(
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class FallthroughExpr(
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class ThisExpr(
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class SuperExpr(
    val scope: ScopeSpecification = CurrentScope
) : IExpression

data class VarExpr(
    val name: String
) : IExpression

data class CallExpr(
    val name: PString,
    val genericParams: List<PType> = emptyList(),
    val params: List<PExpression>
) : IExpression

data class AccessExpr(
    val on: PExpression,
    val name: PString
) : IExpression

data class SafeAccessExpr(
    val on: PExpression,
    val name: PString
) : IExpression

data class MethodCallExpr(
    val on: PExpression,
    val name: PString,
    val genericParams: List<PType> = emptyList(),
    val params: List<PExpression>
) : IExpression

data class SafeMethodCallExpr(
    val on: PExpression,
    val name: PString,
    val genericParams: List<PType> = emptyList(),
    val params: List<PExpression>
) : IExpression

data class GetExpr(
    val on: PExpression,
    val params: List<PExpression>
) : IExpression

data class MethodRefExpr(
    val on: PExpression,
    val name: PString
) : IExpression

data class AnonymousLambdaExpr(
    val scope: NamedScope,
    val params: List<PString>
) : IExpression

data class TypedLambdaExpr(
    val scope: NamedScope,
    val params: List<Pair<PString, PType>>
) : IExpression

data class AnonymousObjectExpr(
    val superTypes: List<SuperType>,
    val scope: NamedScope
) : IExpression