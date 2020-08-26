package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.StringPos

private val number: Parser<Any, Expression> = { state, succ, cErr, eErr ->
    when (val first = state.char) {
        '0' -> when (state.nextChar) {
            'x', 'X' -> handleHexNumber(state.code, state.pos + 2, StringBuilder(), succ, cErr)
            'b', 'B' -> handleBinaryNumber(state.code, state.pos + 2, StringBuilder(), succ, cErr)
            else -> handleNumber(state.code, state.nextPos, StringBuilder("0"), succ, eErr)
        }
        in '1'..'9' ->
            handleNumber(state.code, state.nextPos, StringBuilder().append(first), succ, cErr)
        else -> eErr(missingNumberError, state.area)
    }
}

@Suppress("UNCHECKED_CAST")
fun <R> number() = number as Parser<R, Expression>

private tailrec fun <R> handleNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = code.getOrNull(pos)) {
    in '0'..'9' ->
        handleNumber(code, pos + 1, builder.append(char), succFn, errFn)
    '_' ->
        handleNumber(code, pos + 1, builder, succFn, errFn)
    '.' -> {
        val next = code.getOrNull(pos + 1)
        if (next in '0'..'9') handleDecimalNumber(code, pos + 2, builder.append(char).append(next), succFn, errFn)
        else succFn(convertIntString(builder), ParseState(code, pos))
    }
    'b', 'B' ->
        succFn(ByteExpr(builder.toString().toByte()), ParseState(code, pos + 1))
    's', 'S' ->
        succFn(ShortExpr(builder.toString().toShort()), ParseState(code, pos + 1))
    'i', 'I' ->
        succFn(IntExpr(builder.toString().toInt()), ParseState(code, pos + 1))
    'l', 'L' ->
        succFn(LongExpr(builder.toString().toLong()), ParseState(code, pos + 1))
    'f', 'F' ->
        succFn(FloatExpr(builder.toString().toFloat()), ParseState(code, pos + 1))
    'd', 'D' ->
        succFn(DoubleExpr(builder.toString().toDouble()), ParseState(code, pos + 1))
    in identStartChars ->
        errFn(invalidDecimalLiteralError, pos..pos)
    else ->
        succFn(convertIntString(builder), ParseState(code, pos))
}

private fun convertIntString(builder: StringBuilder): Expression = when {
    builder.length <= 10 -> IntExpr(builder.toString().toInt())
    builder.length <= 19 -> LongExpr(builder.toString().toLong())
    else -> DoubleExpr(builder.toString().toDouble())
}

private tailrec fun <R> handleDecimalNumber(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> handleDecimalNumber(code, pos + 1, builder.append(char), succFn, errFn)
    '_' -> handleDecimalNumber(code, pos + 1, builder, succFn, errFn)
    'e' -> when (val exponentStart = code.getOrNull(pos + 1)) {
        '+', '-' -> {
            val digit = code.getOrNull(pos + 2)
            if (digit?.isDigit() == true)
                handleDecimalExponent(code, pos + 3, builder.append(exponentStart).append(digit), succFn, errFn)
            else errFn(invalidExponentError, (pos + 2).let { it..it })
        }
        in '0'..'9' -> handleDecimalExponent(code, pos + 2, builder.append(exponentStart), succFn, errFn)
        else -> errFn(invalidExponentError, (pos + 1).let { it..it })
    }
    'f', 'F' ->
        succFn(FloatExpr(builder.toString().toFloat()), ParseState(code, pos + 1))
    'd', 'D' ->
        succFn(DoubleExpr(builder.toString().toDouble()),ParseState(code, pos + 1))
    in identStartChars ->
        errFn(invalidDecimalLiteralError, pos..pos)
    else -> succFn(DoubleExpr(builder.toString().toDouble()), ParseState(code, pos))
}

private tailrec fun <R> handleDecimalExponent(
    code: String,
    pos :StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = code.getOrNull(pos)) {
    in '0'..'9' -> handleDecimalExponent(code, pos + 1, builder.append(char), succFn, errFn)
    '_' -> handleDecimalExponent(code, pos + 1, builder, succFn, errFn)
    in identStartChars -> errFn(invalidDecimalLiteralError, pos..pos)
    else -> succFn(DoubleExpr(builder.toString().toDouble()), ParseState(code, pos))
}

private tailrec fun <R> handleBinaryNumber(
    code: String,
    pos :StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = code.getOrNull(pos)) {
    '0', '1' ->
        handleBinaryNumber(code, pos + 1, builder.append(char), succFn, errFn)
    '_' ->
        handleBinaryNumber(code, pos + 1, builder, succFn, errFn)
    'b', 'B' ->
        succFn(ByteExpr(builder.toString().toByte(radix = 2)), ParseState(code, pos))
    's', 'S' ->
        succFn(ShortExpr(builder.toString().toShort(radix = 2)), ParseState(code, pos))
    'i', 'I' ->
        succFn(IntExpr(builder.toString().toInt(radix = 2)), ParseState(code, pos))
    'l', 'L' ->
        succFn(LongExpr(builder.toString().toLong(radix = 2)), ParseState(code, pos + 1))
    in identStartChars ->
        errFn(invalidBinaryLiteralError, pos..pos)
    else ->
        succFn(
            if (builder.length <= 32) IntExpr(builder.toString().toInt(radix = 2))
            else LongExpr(builder.toString().toLong(radix = 2)), ParseState(code, pos)
        )
}

private tailrec fun <R> handleHexNumber(
    code: String,
    pos :StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' ->
        handleHexNumber(code, pos + 1, builder.append(char), succFn, errFn)
    '_' ->
        handleHexNumber(code, pos + 1, builder, succFn, errFn)
    in identStartChars ->
        errFn(invalidHexLiteralError, pos..pos)
    else ->
        succFn(
            if (builder.length <= 8) IntExpr(builder.toString().toInt(radix = 16))
            else LongExpr(builder.toString().toLong(radix = 16)), ParseState(code, pos)
        )
}