package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidBinaryLiteralError
import com.scientianova.palm.errors.invalidDecimalLiteralError
import com.scientianova.palm.errors.invalidExponentError
import com.scientianova.palm.errors.invalidHexLiteralError
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

tailrec fun handleNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> = when (val char = state.char) {
    in '0'..'9' ->
        handleNumber(state.next, startPos, builder.append(char))
    '_' ->
        handleNumber(state.next, startPos, builder)
    '.' -> {
        val next = state.next
        if (next.char in '0'..'9') handleDecimalNumber(next, startPos, builder.append(char))
        else convertIntString(builder) at (startPos until state.pos) succTo state
    }
    'b', 'B' ->
        ByteExpr(builder.toString().toByte()) at startPos..state.pos succTo state.next
    's', 'S' ->
        ShortExpr(builder.toString().toShort()) at startPos..state.pos succTo state.next
    'i', 'I' ->
        IntExpr(builder.toString().toInt()) at startPos..state.pos succTo state.next
    'l', 'L' ->
        LongExpr(builder.toString().toLong()) at startPos..state.pos succTo state.next
    'f', 'F' ->
        FloatExpr(builder.toString().toFloat()) at startPos..state.pos succTo state.next
    'd', 'D' ->
        DoubleExpr(builder.toString().toDouble()) at startPos..state.pos succTo state.next
    in identStartChars ->
        invalidDecimalLiteralError errAt state.pos
    else ->
        convertIntString(builder) at (startPos until state.pos) succTo state
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
): ParseResult<PExpr> = when (val char = state.char) {
    in '0'..'9' -> handleDecimalNumber(state.next, startPos, builder.append(char))
    '_' -> handleDecimalNumber(state.next, startPos, builder)
    'e' -> when (val exponentStart = state.nextChar) {
        '+', '-' -> {
            val digitState = state + 3
            if (digitState.char?.isDigit() == true)
                handleDecimalExponent(digitState, startPos, builder.append(exponentStart))
            else invalidExponentError errAt digitState.pos
        }
        in '0'..'9' -> handleDecimalExponent(state + 2, startPos, builder)
        else -> invalidExponentError errAt state.nextPos
    }
    'f', 'F' ->
        FloatExpr(builder.toString().toFloat()) at startPos..state.pos succTo state.next
    'd', 'D' ->
        DoubleExpr(builder.toString().toDouble()) at startPos..state.pos succTo state.next
    in identStartChars ->
        invalidDecimalLiteralError errAt state.pos
    else -> DoubleExpr(builder.toString().toDouble()) at (startPos until state.pos) succTo state
}

tailrec fun handleDecimalExponent(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> = when (val char = state.char) {
    in '0'..'9' -> handleDecimalExponent(state.next, startPos, builder.append(char))
    '_' -> handleDecimalExponent(state.next, startPos, builder)
    in identStartChars -> invalidDecimalLiteralError errAt state.pos
    else -> DoubleExpr(builder.toString().toDouble()) at (startPos until state.pos) succTo state
}

tailrec fun handleBinaryNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> = when (val char = state.char) {
    '0', '1' ->
        handleBinaryNumber(state.next, startPos, builder.append(char))
    '_' ->
        handleBinaryNumber(state.next, startPos, builder)
    'b', 'B' ->
        ByteExpr(builder.toString().toByte(radix = 2)) at startPos..state.pos succTo state.next
    's', 'S' ->
        ShortExpr(builder.toString().toShort(radix = 2)) at startPos..state.pos succTo state.next
    'i', 'I' ->
        IntExpr(builder.toString().toInt(radix = 2)) at startPos..state.pos succTo state.next
    'l', 'L' ->
        LongExpr(builder.toString().toLong(radix = 2)) at startPos..state.lastPos succTo state.next
    in identStartChars ->
        invalidBinaryLiteralError errAt state.pos
    else ->
        (if (builder.length <= 32) IntExpr(builder.toString().toInt(radix = 2))
        else LongExpr(builder.toString().toLong(radix = 2))) at (startPos until state.pos) succTo state
}

tailrec fun handleHexNumber(
    state: ParseState,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PExpr> = when (val char = state.char) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' ->
        handleHexNumber(state.next, startPos, builder.append(char))
    '_' ->
        handleHexNumber(state.next, startPos, builder)
    in identStartChars ->
        invalidHexLiteralError errAt state.pos
    else ->
        (if (builder.length <= 8) IntExpr(builder.toString().toInt(radix = 16))
        else LongExpr(builder.toString().toLong(radix = 16))) at (startPos until state.pos) succTo state
}