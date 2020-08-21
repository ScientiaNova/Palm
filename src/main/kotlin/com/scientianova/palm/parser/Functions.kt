package com.scientianova.palm.parser

import com.scientianova.palm.errors.missingIdentifierError
import com.scientianova.palm.errors.unknownParamModifierError
import com.scientianova.palm.util.PString

enum class ParamModifier {
    Using, Given
}

sealed class FunDec<T> {
    abstract val name: PString
    abstract val params: List<FunParam>
    abstract val info: T
}
data class Function<T>(
    override val name: PString,
    override val params: List<FunParam>,
    override val info: T,
    val type: PType?,
    val expr: PExpr
) : FunDec<T>()

data class FunParam(
    val modifier: List<ParamModifier>,
    val name: PString,
    val type: PType,
    val default: PExpr? = null
)

fun expectIdent(state: ParseState): ParseResult<PString> = when (state.char) {
    in identStartChars -> handleIdent(state).toResult()
    '`' -> handleBacktickedIdent(state)
    else -> missingIdentifierError failAt state
}

fun handleParam(
    startState: ParseState
): ParseResult<FunParam> = reuseWhileSuccess(startState, emptyList<ParamModifier>()) { mods, state ->
    expectIdent(state).flatMap { ident, afterIdent ->
        val maybeColon = afterIdent.actual
        if (maybeColon.char == ':') handleType(maybeColon.nextActual, false).flatMap { type, afterType ->
            val maybeEq = afterType.actual
            if (maybeEq.char == '=') handleInlinedExpr(maybeEq.nextActual, false).flatMap { expr, afterExpr ->
                return FunParam(mods, ident, type, expr) succTo afterExpr
            } else return FunParam(mods, ident, type) succTo afterType
        } else handleParamModifier(ident, maybeColon).map(mods::plus)
    }
}

fun handleParamModifier(ident: PString, nextState: ParseState) = when (ident.value) {
    "using" -> ParamModifier.Using succTo nextState
    "given" -> ParamModifier.Given succTo nextState
    else -> unknownParamModifierError failAt ident.area
}

data class FileFunctionInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val tailRec: Boolean,
    val given: Boolean,
    val using: Boolean
)

data class MethodInfo(
    val privacy: ClassLevelPrivacy,
    val abstract: Boolean,
    val tailRec: Boolean,
    val given: Boolean,
    val using: Boolean
)