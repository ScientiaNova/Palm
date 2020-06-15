package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.INVALID_BINARY_LITERAL_ERROR
import com.scientianova.palm.errors.INVALID_DECIMAL_LITERAL_ERROR
import com.scientianova.palm.errors.INVALID_EXPONENT_ERROR
import com.scientianova.palm.errors.INVALID_HEX_LITERAL_ERROR
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

tailrec fun handleNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PToken, Char?> = when {
    char in '0'..'9' ->
        handleNumber(traverser, traverser.pop(), startPos, builder.append(char))
    char == '_' ->
        handleNumber(traverser, traverser.pop(), startPos, builder)
    char == '.' ->
        if (traverser.peek()?.isDigit() == true)
            handleDecimalNumber(traverser, traverser.pop(), startPos, builder.append(char))
        else convertIntString(builder) at (startPos until traverser.lastPos) to char
    char == 'b' || char == 'B' ->
        ByteToken(builder.toString().toByte()) at startPos..traverser.lastPos to traverser.pop()
    char == 's' || char == 'S' ->
        ShortToken(builder.toString().toShort()) at startPos..traverser.lastPos to traverser.pop()
    char == 'i' || char == 'I' ->
        IntToken(builder.toString().toInt()) at startPos..traverser.lastPos to traverser.pop()
    char == 'l' || char == 'L' ->
        LongToken(builder.toString().toLong()) at startPos..traverser.lastPos to traverser.pop()
    char == 'f' || char == 'F' ->
        FloatToken(builder.toString().toFloat()) at startPos..traverser.lastPos to traverser.pop()
    char == 'd' || char == 'D' ->
        DoubleToken(builder.toString().toDouble()) at startPos..traverser.lastPos to traverser.pop()
    char?.isLetter() == true ->
        traverser.error(INVALID_DECIMAL_LITERAL_ERROR, traverser.lastPos)
    else ->
        convertIntString(builder) at (startPos until traverser.lastPos) to char
}

fun convertIntString(builder: StringBuilder): Token = when {
    builder.length <= 10 -> IntToken(builder.toString().toInt())
    builder.length <= 19 -> LongToken(builder.toString().toLong())
    else -> DoubleToken(builder.toString().toDouble())
}

tailrec fun handleDecimalNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder(".")
): Pair<PToken, Char?> = when {
    char in '0'..'9' -> handleDecimalNumber(traverser, traverser.pop(), startPos, builder.append(char))
    char == '_' -> handleDecimalNumber(traverser, traverser.pop(), startPos, builder)
    char == 'e' -> when (traverser.peek()) {
        '+', '-' -> {
            val symbol = traverser.pop()
            if (traverser.peek()?.isDigit() == true)
                handleDecimalExponent(traverser, traverser.pop(), startPos, builder.append(symbol))
            else traverser.error(INVALID_EXPONENT_ERROR, traverser.lastPos + 1)
        }
        in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder)
        else -> traverser.error(INVALID_EXPONENT_ERROR, traverser.lastPos + 1)
    }
    char == 'f' || char == 'F' ->
        FloatToken(builder.toString().toFloat()) at startPos..traverser.lastPos to traverser.pop()
    char == 'd' || char == 'D' ->
        DoubleToken(builder.toString().toDouble()) at startPos..traverser.lastPos to traverser.pop()
    char?.isLetter() == true ->
        traverser.error(INVALID_DECIMAL_LITERAL_ERROR, traverser.lastPos)
    else -> DoubleToken(builder.toString().toDouble()) at (startPos until traverser.lastPos) to char
}

tailrec fun handleDecimalExponent(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder(".")
): Pair<PToken, Char?> = when {
    char in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder.append(char))
    char == '_' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder)
    char?.isLetter() == true -> traverser.error(INVALID_DECIMAL_LITERAL_ERROR, traverser.lastPos)
    else -> DoubleToken(builder.toString().toDouble()) at (startPos until traverser.lastPos) to char
}

tailrec fun handleBinaryNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PToken, Char?> = when {
    char == '0' || char == '1' ->
        handleBinaryNumber(traverser, traverser.pop(), startPos, builder.append(char))
    char == '_' ->
        handleBinaryNumber(traverser, traverser.pop(), startPos, builder)
    char == 'b' || char == 'B' ->
        ByteToken(builder.toString().toByte(radix = 2)) at startPos..traverser.lastPos to traverser.pop()
    char == 's' || char == 'S' ->
        ShortToken(builder.toString().toShort(radix = 2)) at startPos..traverser.lastPos to traverser.pop()
    char == 'i' || char == 'I' ->
        IntToken(builder.toString().toInt(radix = 2)) at startPos..traverser.lastPos to traverser.pop()
    char == 'l' || char == 'L' ->
        LongToken(builder.toString().toLong(radix = 2)) at startPos..traverser.lastPos to traverser.pop()
    char?.isLetter() == true ->
        traverser.error(INVALID_BINARY_LITERAL_ERROR, traverser.lastPos)
    else ->
        (if (builder.length <= 32) IntToken(builder.toString().toInt(radix = 2))
        else LongToken(builder.toString().toLong(radix = 2))) at (startPos until traverser.lastPos) to char
}

tailrec fun handleHexNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PToken, Char?> = when {
    char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F' ->
        handleHexNumber(traverser, traverser.pop(), startPos, builder.append(char))
    char == '_' ->
        handleHexNumber(traverser, traverser.pop(), startPos, builder)
    char?.isLetter() == true ->
        traverser.error(INVALID_HEX_LITERAL_ERROR, traverser.lastPos)
    else ->
        (if (builder.length <= 8) IntToken(builder.toString().toInt(radix = 16))
        else LongToken(builder.toString().toLong(radix = 16))) at (startPos until traverser.lastPos) to char
}