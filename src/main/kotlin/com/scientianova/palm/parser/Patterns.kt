package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidDoubleDeclarationError
import com.scientianova.palm.errors.unclosedParenthesisError
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.util.*

sealed class Pattern
typealias PPattern = Positioned<Pattern>

data class ExprPattern(val expr: Expression) : Pattern()
data class DecPattern(val name: String, val mutable: Boolean) : Pattern()
data class EnumPattern(val name: PString, val params: List<PPattern>) : Pattern()
data class TypePattern(val type: PType) : Pattern()

object WildcardPattern : Pattern()

enum class DeclarationType { VAR, VAL, NONE }

fun handlePattern(state: ParseState, decType: DeclarationType): Pair<PPattern, ParseState> = when {
    state.char == '.' && state.nextChar?.isLetter() == true -> {
        val (ident, afterIdent) = handleIdentifier(state.next)
        val maybeParen = afterIdent.actual
        if (maybeParen.char == '(') {
            val (components, next) = handlePatternTuple(maybeParen.nextActual, decType, maybeParen.pos, emptyList())
            EnumPattern(ident, components) at state.pos..next.lastPos to next
        } else EnumPattern(ident, emptyList()) at state.pos..ident.area.last to afterIdent
    }
    state.char?.isLetter() == true -> {
        val (ident, afterIdent) = handleIdentifier(state)
        when (ident.value) {
            "_" -> WildcardPattern at ident.area to afterIdent
            "is" -> {
                val (type, afterType) = handleType(afterIdent.actual)
                TypePattern(type) at state.pos..type.area.last to afterType
            }
            "val" ->
                if (decType == DeclarationType.NONE) handlePattern(afterIdent.actual, DeclarationType.VAL)
                else invalidDoubleDeclarationError throwAt ident.area
            "var" ->
                if (decType == DeclarationType.NONE) handlePattern(afterIdent.actual, DeclarationType.VAR)
                else invalidDoubleDeclarationError throwAt ident.area
            else -> {
                val (expr, afterExpr) = handleInlinedBinOps(afterIdent.actual, ident.map(::IdentExpr), false)
                if (expr.value is IdentExpr) when (decType) {
                    DeclarationType.NONE -> expr.map(::ExprPattern) to afterExpr
                    DeclarationType.VAL -> DecPattern(expr.value.name, false) at expr.area to afterExpr
                    DeclarationType.VAR -> DecPattern(expr.value.name, true) at expr.area to afterExpr
                } else expr.map(::ExprPattern) to afterExpr
            }
        }
    }
    else -> {
        val (expr, afterExpr) = handleInlinedExpression(state, false)
        if (expr.value is IdentExpr) when (decType) {
            DeclarationType.NONE -> expr.map(::ExprPattern) to afterExpr
            DeclarationType.VAL -> DecPattern(expr.value.name, false) at expr.area to afterExpr
            DeclarationType.VAR -> DecPattern(expr.value.name, true) at expr.area to afterExpr
        } else expr.map(::ExprPattern) to afterExpr
    }
}

tailrec fun handlePatternTuple(
    state: ParseState,
    decType: DeclarationType,
    startPos: StringPos,
    patterns: List<PPattern>
): Pair<List<PPattern>, ParseState> = if (state.char == ')') patterns to state.next else {
    val (pattern, afterPattern) = handlePattern(state, decType)
    val symbol = afterPattern.actual
    when (symbol.char) {
        ',' -> handlePatternTuple(symbol.nextActual, decType, startPos, patterns + pattern)
        ')' -> (patterns + pattern) to symbol.next
        else -> unclosedParenthesisError throwAt symbol.pos
    }
}