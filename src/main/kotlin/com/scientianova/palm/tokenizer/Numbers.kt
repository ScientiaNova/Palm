package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

fun handleNumber(
    traverser: StringTraverser,
    char: Char?,
    list: TokenList,
    startPos: StringPos = traverser.lastPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9' -> handleNumber(traverser, traverser.pop(), list, startPos, builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true)
            handleNumber(traverser, traverser.pop(), list, startPos, builder)
        else NumberToken(convertIntString(builder)) on startPos..traverser.lastPos.shift(-1) to char
    '.' ->
        if (traverser.peek()?.isDigit() == true)
            handleDecimalNumber(traverser, traverser.pop(), list, startPos, builder.append(char))
        else NumberToken(convertIntString(builder)) on startPos..traverser.lastPos.shift(-1) to char
    else ->
        if (char?.isLetter() == true) {
            list.offer(NumberToken(convertIntString(builder)) on startPos..traverser.lastPos.shift(-1))
            TimesToken on traverser.lastPos to char
        } else NumberToken(convertIntString(builder)) on startPos..traverser.lastPos.shift(-1) to char
}

fun convertIntString(builder: StringBuilder): Number = when {
    builder.length <= 10 -> builder.toString().toInt()
    builder.length <= 19 -> builder.toString().toLong()
    else -> builder.toString().toDouble()
}

fun handleDecimalNumber(
    traverser: StringTraverser,
    char: Char?,
    list: TokenList,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder(".")
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9' -> handleDecimalNumber(traverser, traverser.pop(), list, startPos, builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true)
            handleDecimalNumber(traverser, traverser.pop(), list, startPos, builder)
        else NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
    'e' -> when (traverser.peek()) {
        '+', '-' -> {
            val symbol = traverser.pop()
            if (traverser.peek()?.isDigit() == true)
                handleDecimalExponent(traverser, traverser.pop(), startPos, builder.append(symbol))
            else {
                list.offer(NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1))
                list.offer(IdentifierToken("e") on traverser.lastPos.shift(-1))
                handleSymbol(traverser, symbol)
            }
        }
        in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder)
        else -> NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
    }
    else -> if (char?.isLetter() == true) {
        list.offer(NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1))
        TimesToken on traverser.lastPos to char
    } else NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
}

fun handleDecimalExponent(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder(".")
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), startPos, builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleDecimalExponent(traverser, traverser.pop(), startPos, builder)
        else NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
    else -> NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
}

fun handleBinaryNumber(
    traverser: StringTraverser,
    char: Char?,
    list: TokenList,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> = when (char) {
    '0', '1' -> handleBinaryNumber(traverser, traverser.pop(), list, startPos, builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleBinaryNumber(traverser, traverser.pop(), list, startPos, builder)
        else NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
    else -> {
        val token = NumberToken(
            if (builder.length <= 32) builder.toString().toInt(radix = 2) else builder.toString().toLong(radix = 2)
        )
        (if (char?.isLetter() == true) {
            list.offer(token on startPos..traverser.lastPos.shift(-1))
            TimesToken on traverser.lastPos
        } else token on startPos..traverser.lastPos.shift(-1)) to char
    }
}

fun handleHexNumber(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> = when (char) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> handleHexNumber(traverser, traverser.pop(), startPos, builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleHexNumber(traverser, traverser.pop(), startPos, builder)
        else NumberToken(builder.toString().toDouble()) on startPos..traverser.lastPos.shift(-1) to char
    else -> NumberToken(
        if (builder.length <= 8) builder.toString().toInt(radix = 16) else builder.toString().toLong(radix = 16)
    ) on startPos..traverser.lastPos.shift(-1) to char
}