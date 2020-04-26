package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.INVALID_EXPONENT_ERROR
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

sealed class NumberToken : IToken
data class ByteToken(val value: Byte) : NumberToken()
data class ShortToken(val value: Short) : NumberToken()
data class IntToken(val value: Int) : NumberToken()
data class LongToken(val value: Long) : NumberToken()
data class FloatToken(val value: Float) : NumberToken()
data class DoubleToken(val value: Double) : NumberToken()

fun handleNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos = traverser.lastPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9' -> handleNumber(traverser, traverser.pop(), startPos, builder.append(char))
    '_' -> handleNumber(traverser, traverser.pop(), startPos, builder)
    '.' ->
        if (traverser.peek()?.isDigit() == true)
            handleDecimalNumber(traverser, traverser.pop(), startPos, builder.append(char))
        else convertIntString(builder) on startPos..traverser.lastPos.shift(-1) to char
    'b', 'B' -> ByteToken(builder.toString().toByte()) on startPos..traverser.lastPos to traverser.pop()
    's', 'S' -> ShortToken(builder.toString().toShort()) on startPos..traverser.lastPos to traverser.pop()
    'i', 'I' -> IntToken(builder.toString().toInt()) on startPos..traverser.lastPos to traverser.pop()
    'f', 'F' -> FloatToken(builder.toString().toFloat()) on startPos..traverser.lastPos to traverser.pop()
    'd', 'D' -> DoubleToken(builder.toString().toDouble()) on startPos..traverser.lastPos to traverser.pop()
    else -> convertIntString(builder) on startPos..traverser.lastPos.shift(-1) to char
}

fun convertIntString(builder: StringBuilder): NumberToken = when {
    builder.length <= 10 -> IntToken(builder.toString().toInt())
    builder.length <= 19 -> LongToken(builder.toString().toLong())
    else -> DoubleToken(builder.toString().toDouble())
}

fun handleDecimalNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder(".")
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9' -> handleDecimalNumber(traverser, traverser.pop(), startPos, builder.append(char))
    '_' -> handleDecimalNumber(traverser, traverser.pop(), startPos, builder)
    'e' -> when (traverser.peek()) {
        '+', '-' -> {
            val symbol = traverser.pop()
            if (traverser.peek()?.isDigit() == true)
                handleDecimalExponent(traverser, traverser.pop(), startPos, builder.append(symbol))
            else traverser.error(INVALID_EXPONENT_ERROR, traverser.lastPos.shift(1))
        }
        in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder)
        else -> traverser.error(INVALID_EXPONENT_ERROR, traverser.lastPos.shift(1))
    }
    'f', 'F' -> FloatToken(builder.toString().toFloat()) on startPos..traverser.lastPos to traverser.pop()
    'd', 'D' -> FloatToken(builder.toString().toFloat()) on startPos..traverser.lastPos to traverser.pop()
    else -> DoubleToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
}

fun handleDecimalExponent(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder(".")
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder.append(char))
    '_' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder)
    else -> DoubleToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
}

fun handleBinaryNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> = when (char) {
    '0', '1' -> handleBinaryNumber(traverser, traverser.pop(), startPos, builder.append(char))
    '_' -> handleBinaryNumber(traverser, traverser.pop(), startPos, builder)
    else -> (if (builder.length <= 32) IntToken(builder.toString().toInt(radix = 2))
    else LongToken(builder.toString().toLong(radix = 2))) on startPos..traverser.lastPos.shift(-1) to char

}

fun handleHexNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> handleHexNumber(traverser, traverser.pop(), startPos, builder.append(char))
    '_' -> handleHexNumber(traverser, traverser.pop(), startPos, builder)
    else -> (if (builder.length <= 8) IntToken(builder.toString().toInt(radix = 16))
    else LongToken(builder.toString().toLong(radix = 16))) on startPos..traverser.lastPos.shift(-1) to char
}