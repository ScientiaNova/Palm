@file:Suppress("UNCHECKED_CAST")

package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidDoubleDeclarationError
import com.scientianova.palm.errors.invalidPatternError
import com.scientianova.palm.errors.missingPatternError
import com.scientianova.palm.errors.unclosedParenthesisError
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.at

sealed class Pattern
typealias PPattern = Positioned<Pattern>

data class ExprPattern(val expr: Expression) : Pattern()
data class DecPattern(val name: String, val mutable: Boolean) : Pattern()
data class EnumPattern(val name: PString, val params: List<PPattern>) : Pattern()
data class TypePattern(val type: PType) : Pattern()

object WildcardPattern : Pattern()

fun <R> varPattern() = varPattern as Parser<R, PPattern>
fun <R> valPattern() = valPattern as Parser<R, PPattern>
fun <R> pattern() = pattern as Parser<R, PPattern>

fun <R> varPatternNoExpr() = varPatternNoExpr as Parser<R, PPattern>
fun <R> valPatternNoExpr() = valPatternNoExpr as Parser<R, PPattern>

private val varPattern: Parser<Any, PPattern> = oneOfOrError(
    missingPatternError,
    enumPattern(::varPattern).withPos(),
    normalIdentifier<Any>().withPos().flatMap { ident ->
        when (ident.value) {
            "_" -> valueP(WildcardPattern at ident.area)
            "val" -> consumedError(invalidDoubleDeclarationError, ident.area)
            "var" -> consumedError(invalidDoubleDeclarationError, ident.area)
            else -> inlineIdentExpr<Any>(ident).map {
                val expr = it.value
                if (expr is IdentExpr) {
                    DecPattern(expr.name, true)
                } else {
                    ExprPattern(it.value)
                } at it.area
            }
        }
    },
    inlineExpr<Any>().map {
        val expr = it.value
        if (expr is IdentExpr) {
            DecPattern(expr.name, true)
        } else {
            ExprPattern(it.value)
        } at it.area
    }
)

private val valPattern: Parser<Any, PPattern> = oneOfOrError(
    missingPatternError,
    enumPattern(::valPattern).withPos(),
    normalIdentifier<Any>().withPos().flatMap { ident ->
        when (ident.value) {
            "_" -> valueP(WildcardPattern at ident.area)
            "val" -> consumedError(invalidDoubleDeclarationError, ident.area)
            "var" -> consumedError(invalidDoubleDeclarationError, ident.area)
            else -> inlineIdentExpr<Any>(ident).map {
                val expr = it.value
                if (expr is IdentExpr) {
                    DecPattern(expr.name, false)
                } else {
                    ExprPattern(it.value)
                } at it.area
            }
        }
    },
    inlineExpr<Any>().map {
        val expr = it.value
        if (expr is IdentExpr) {
            DecPattern(expr.name, false)
        } else {
            ExprPattern(it.value)
        } at it.area
    }
)

private val pattern: Parser<Any, PPattern> = oneOfOrError(
    missingPatternError,
    enumPattern(::pattern).withPos(),
    normalIdentifier<Any>().withPos().flatMap { ident ->
        when (ident.value) {
            "_" -> valueP(WildcardPattern at ident.area)
            "val" -> whitespace<Any>().takeR(valPattern)
            "var" -> whitespace<Any>().takeR(varPattern)
            else -> inlineIdentExpr<Any>(ident).map { ExprPattern(it.value) at it.area }
        }
    },
    inlineExpr<Any>().map { ExprPattern(it.value) at it.area }
)

private val varPatternNoExpr: Parser<Any, PPattern> = oneOfOrError(
    missingPatternError,
    enumPattern(::varPattern).withPos(),
    normalIdentifier<Any>().withPos().flatMap { ident ->
        when (ident.value) {
            "_" -> valueP(WildcardPattern at ident.area)
            "val" -> consumedError(invalidDoubleDeclarationError, ident.area)
            "var" -> consumedError(invalidDoubleDeclarationError, ident.area)
            else -> consumedError(invalidPatternError, ident.area)
        }
    }
)

private val valPatternNoExpr: Parser<Any, PPattern> = oneOfOrError(
    missingPatternError,
    enumPattern(::valPattern).withPos(),
    normalIdentifier<Any>().withPos().flatMap { ident ->
        when (ident.value) {
            "_" -> valueP(WildcardPattern at ident.area)
            "val" -> consumedError(invalidDoubleDeclarationError, ident.area)
            "var" -> consumedError(invalidDoubleDeclarationError, ident.area)
            else -> consumedError(invalidPatternError, ident.area)
        }
    }
)

private fun patternTuple(patternP: () -> Parser<ParseResult<PPattern>, PPattern>): Parser<Any, List<PPattern>> =
    tryChar<Any>('(').takeRLazy { loopingBodyParser(')', patternP(), unclosedParenthesisError) }

private fun enumPattern(patternP: () -> Parser<ParseResult<PPattern>, PPattern>) = tryChar<Any>('.')
    .takeR(identifier<Any>().withPos())
    .zipWith(patternTuple(patternP).orDefault(emptyList()), ::EnumPattern)