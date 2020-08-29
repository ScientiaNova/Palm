@file:Suppress("UNCHECKED_CAST")

package com.scientianova.palm.parser

import com.scientianova.palm.errors.missingTypeError
import com.scientianova.palm.errors.missingTypeReturnTypeError
import com.scientianova.palm.errors.unclosedParenthesisError
import com.scientianova.palm.errors.unclosedSquareBacketError
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Type
typealias PType = Positioned<Type>

typealias Path = List<PString>

data class NamedType(
    val path: Path,
    val generics: List<PType> = emptyList()
) : Type()

data class FunctionType(
    val params: List<PType>,
    val returnType: PType,
    val implicit: Boolean
) : Type()

private inline fun type(noinline whitespaceP: Parser<Any, Unit>, crossinline typePFn: () -> Parser<Any, Type>) =
    oneOfOrError(
        missingTypeError,
        identifier<Any>().withPos().zipWith(pathTail) { start, tail ->
            listOf(start, *tail.toTypedArray())
        }.zipWith(whitespaceP.takeR(generics), ::NamedType),
        parenthesizedType(whitespaceP, typePFn)
    )

fun <R> inlinedType(): Parser<R, Type> = inlinedType as Parser<R, Type>
private val inlinedType: Parser<Any, Type> by lazy { type(whitespace()) { inlinedType() } }

fun <R> scopedType(): Parser<R, Type> = scopedType as Parser<R, Type>
private val scopedType: Parser<Any, Type> by lazy { type(whiteSpaceOnLine()) { scopedType() } }

private val typeAnnotation: Parser<Any, PType> by lazy {
    matchChar<Any>(':').takeR(whitespace()).takeR(elevateError(scopedType())).withPos()
}

fun <R> typeAnnotation() = typeAnnotation as Parser<R, PType>

private val pathNode: Parser<Any, PString> = whitespace<Any>()
    .takeL(matchChar('.'))
    .takeR(whitespace())
    .takeR(elevateError(identifier()))
    .withPos()

fun <R> pathNode() = pathNode as Parser<R, PString>

private val pathTail: Parser<Any, List<PString>> = parser@{ startState, succ, cErr, _ ->
    loopValue(listOf<PString>() to startState) { (list, state) ->
        when (val res = returnResultT(state, pathNode())) {
            is ParseResultT.Success -> (list + res.value) to res.next
            is ParseResultT.Error -> return@parser cErr(res.error, res.area)
            is ParseResultT.Failure -> return@parser succ(list, state)
        }
    }
}

private val generics: Parser<Any, List<PType>> = matchChar<Any>('[')
    .takeR(loopingBodyParser(']', inlinedType<ParseResult<PType>>().withPos(), unclosedSquareBacketError))

private inline fun parenthesizedType(
    noinline whitespaceP: Parser<Any, Unit>,
    crossinline typePFn: () -> Parser<Any, Type>
): Parser<Any, Type> = matchChar<Any>('(')
    .takeR(loopingBodyParser(')', inlinedType<ParseResult<PType>>().withPos(), unclosedParenthesisError))
    .flatMap { list ->
        oneOf(
            whitespace<Any>().takeR(
                oneOf(
                    matchString<Any>("->").map { false },
                    matchString<Any>("=>").map { true },
                )
            ).zipWith(whitespaceP.takeR(typePFn()).withPos()) { implicit, returnType ->
                FunctionType(list, returnType, implicit)
            },
            { state, succ: SuccFn<Any, Type>, cErr, _ ->
                if (list.size == 1) succ(list.first().value, state)
                else cErr(missingTypeReturnTypeError, state.area)
            }
        )
    }