package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidDoubleDeclarationError
import com.scientianova.palm.errors.invalidPatternError
import com.scientianova.palm.errors.unclosedParenthesisError
import com.scientianova.palm.util.*

sealed class Pattern
typealias PPattern = Positioned<Pattern>

data class ExprPattern(val expr: Expression) : Pattern()
data class DecPattern(val name: String, val mutable: Boolean) : Pattern()
data class EnumPattern(val name: PString, val params: List<PPattern>) : Pattern()
data class TypePattern(val type: PType) : Pattern()

object WildcardPattern : Pattern()

enum class DeclarationType { VAR, VAL, NONE }

fun handlePattern(
    state: ParseState,
    decType: DeclarationType,
    excludeExpression: Boolean
): ParseResult<PPattern> = when {
    state.char == '.' && state.nextChar?.isIdentifierStart() == true -> {
        val (ident, afterIdent) = handleIdent(state.next)
        val maybeParen = afterIdent.actual
        if (maybeParen.char == '(') handlePatternTuple(maybeParen.nextActual, decType, maybeParen.pos, emptyList())
            .flatMap { components, next ->
                EnumPattern(ident, components) at state.pos..next.lastPos succTo next
            }
        else EnumPattern(ident, emptyList()) at state.pos..ident.area.last succTo afterIdent
    }
    state.char?.isIdentifierStart() == true -> {
        val (ident, afterIdent) = handleIdent(state)
        when (ident.value) {
            "_" -> WildcardPattern at ident.area succTo afterIdent
            "is" -> handleType(afterIdent.actual, false).map { type ->
                TypePattern(type) at state.pos..type.area.last
            }
            "val" ->
                if (decType == DeclarationType.NONE) handlePattern(afterIdent.actual, DeclarationType.VAL, false)
                else invalidDoubleDeclarationError errAt ident.area
            "var" ->
                if (decType == DeclarationType.NONE) handlePattern(afterIdent.actual, DeclarationType.VAR, false)
                else invalidDoubleDeclarationError errAt ident.area
            else -> if (excludeExpression) {
                invalidPatternError errAt state.pos
            } else ident.startExpr(afterIdent, false).map { expr ->
                if (expr.value is IdentExpr) when (decType) {
                    DeclarationType.NONE -> expr.map(::ExprPattern)
                    DeclarationType.VAL -> DecPattern(expr.value.name, false) at expr.area
                    DeclarationType.VAR -> DecPattern(expr.value.name, true) at expr.area
                } else expr.map(::ExprPattern)
            }
        }
    }
    else -> handleInlinedExpr(state, false).map { expr ->
        if (expr.value is IdentExpr) when (decType) {
            DeclarationType.NONE -> expr.map(::ExprPattern)
            DeclarationType.VAL -> DecPattern(expr.value.name, false) at expr.area
            DeclarationType.VAR -> DecPattern(expr.value.name, true) at expr.area
        } else expr.map(::ExprPattern)
    }
}

fun handlePatternTuple(
    state: ParseState,
    decType: DeclarationType,
    startPos: StringPos,
    patterns: List<PPattern>
): ParseResult<List<PPattern>> = if (state.char == ')') patterns succTo state.next
else handlePattern(state, decType, false).flatMap { pattern, afterPattern ->
    val symbolState = afterPattern.actual
    when (symbolState.char) {
        ',' -> handlePatternTuple(symbolState.nextActual, decType, startPos, patterns + pattern)
        ')' -> (patterns + pattern) succTo symbolState.next
        else -> unclosedParenthesisError errAt symbolState.pos
    }
}