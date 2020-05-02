package com.scientianova.palm.parser

import com.scientianova.palm.tokenizer.DefinitionModifier
import com.scientianova.palm.util.Positioned

interface IPattern
typealias PPattern = Positioned<IPattern>

data class ExpressionPattern(
    val expr: PExpression
) : IPattern

data class InPattern(
    val expr: PExpression
) : IPattern

object WildcardPattern : IPattern

data class ComponentsPattern(
    val components: List<PPattern>
) : IPattern

data class DeclarationPattern(
    val name: String,
    val modifiers: List<DefinitionModifier>
) : IPattern

data class TypePattern(
    val type: PType,
    val components: List<PPattern>
) : IPattern

sealed class Condition
data class ExpressionCondition(val expr: PExpression) : Condition()
data class LetCondition(val pattern: Positioned<TypePattern>, val expr: PExpression) : Condition()