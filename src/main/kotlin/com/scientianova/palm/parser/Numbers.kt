package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

tailrec fun handleNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> {
    val char = state.char
    return when {
        char in '0'..'9' ->
            handleNumber(state.next, startPos, builder.append(char))
        char == '_' ->
            handleNumber(state.next, startPos, builder)
        char == '.' -> {
            val next = state.next
            if (next.char in '0'..'9') handleDecimalNumber(next, startPos, builder.append(char))
            else convertIntString(builder) at (startPos until state.pos) succTo state
        }
        char == 'b' || char == 'B' ->
            ByteExpr(builder.toString().toByte()) at startPos..state.pos succTo state.next
        char == 's' || char == 'S' ->
            ShortExpr(builder.toString().toShort()) at startPos..state.pos succTo state.next
        char == 'i' || char == 'I' ->
            IntExpr(builder.toString().toInt()) at startPos..state.pos succTo state.next
        char == 'l' || char == 'L' ->
            LongExpr(builder.toString().toLong()) at startPos..state.pos succTo state.next
        char == 'f' || char == 'F' ->
            FloatExpr(builder.toString().toFloat()) at startPos..state.pos succTo state.next
        char == 'd' || char == 'D' ->
            DoubleExpr(builder.toString().toDouble()) at startPos..state.pos succTo state.next
        char?.isLetter() == true ->
            invalidDecimalLiteralError errAt state.pos
        else ->
            convertIntString(builder) at (startPos until state.pos) succTo state
    }
}

fun convertIntString(builder: StringBuilder): Expression = when {
    builder.length <= 10 -> IntExpr(builder.toString().toInt())
    builder.length <= 19 -> LongExpr(builder.toString().toLong())
    else -> DoubleExpr(builder.toString().toDouble())
}

tailrec fun handleDecimalNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> {
    val char = state.char
    return when {
        char in '0'..'9' -> handleDecimalNumber(state.next, startPos, builder.append(char))
        char == '_' -> handleDecimalNumber(state.next, startPos, builder)
        char == 'e' -> when (val exponentStart = state.nextChar) {
            '+', '-' -> {
                val digitState = state + 3
                if (digitState.char?.isDigit() == true)
                    handleDecimalExponent(digitState, startPos, builder.append(exponentStart))
                else invalidExponentError errAt digitState.pos
            }
            in '0'..'9' -> handleDecimalExponent(state + 2, startPos, builder)
            else -> invalidExponentError errAt state.nextPos
        }
        char == 'f' || char == 'F' ->
            FloatExpr(builder.toString().toFloat()) at startPos..state.pos succTo state.next
        char == 'd' || char == 'D' ->
            DoubleExpr(builder.toString().toDouble()) at startPos..state.pos succTo state.next
        char?.isLetter() == true ->
            invalidDecimalLiteralError errAt state.pos
        else -> DoubleExpr(builder.toString().toDouble()) at (startPos until state.pos) succTo state
    }
}

tailrec fun handleDecimalExponent(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> {
    val char = state.char
    return when {
        char in '0'..'9' -> handleDecimalExponent(state.next, startPos, builder.append(char))
        char == '_' -> handleDecimalExponent(state.next, startPos, builder)
        char?.isLetter() == true -> invalidDecimalLiteralError errAt state.pos
        else -> DoubleExpr(builder.toString().toDouble()) at (startPos until state.pos) succTo state
    }
}

tailrec fun handleBinaryNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> {
    val char = state.char
    return when {
        char == '0' || char == '1' ->
            handleBinaryNumber(state.next, startPos, builder.append(char))
        char == '_' ->
            handleBinaryNumber(state.next, startPos, builder)
        char == 'b' || char == 'B' ->
            ByteExpr(builder.toString().toByte(radix = 2)) at startPos..state.pos succTo state.next
        char == 's' || char == 'S' ->
            ShortExpr(builder.toString().toShort(radix = 2)) at startPos..state.pos succTo state.next
        char == 'i' || char == 'I' ->
            IntExpr(builder.toString().toInt(radix = 2)) at startPos..state.pos succTo state.next
        char == 'l' || char == 'L' ->
            LongExpr(builder.toString().toLong(radix = 2)) at startPos..state.lastPos succTo state.next
        char?.isLetter() == true ->
            invalidBinaryLiteralError errAt state.pos
        else ->
            (if (builder.length <= 32) IntExpr(builder.toString().toInt(radix = 2))
            else LongExpr(builder.toString().toLong(radix = 2))) at (startPos until state.pos) succTo state
    }
}

tailrec fun handleHexNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> {
    val char = state.char
    return when {
        char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F' ->
            handleHexNumber(state.next, startPos, builder.append(char))
        char == '_' ->
            handleHexNumber(state.next, startPos, builder)
        char?.isLetter() == true ->
            invalidHexLiteralError errAt state.pos
        else ->
            (if (builder.length <= 8) IntExpr(builder.toString().toInt(radix = 16))
            else LongExpr(builder.toString().toLong(radix = 16))) at (startPos until state.pos) succTo state
    }
}