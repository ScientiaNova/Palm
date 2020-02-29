package com.scientianovateam.palm.tokenizer

fun handleNumber(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<NumberToken, Char?> = when (char) {
    in '0'..'9' -> handleNumber(traverser, traverser.pop(), builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleNumber(traverser, traverser.pop(), builder)
        else NumberToken(builder.toString().toDouble()) to char
    '.' ->
        if (traverser.peek()?.isDigit() == true) handleDecimalNumber(traverser, traverser.pop(), builder.append(char))
        else NumberToken(builder.toString().toDouble()) to char
    else -> NumberToken(builder.toString().toDouble()) to char
}

fun handleDecimalNumber(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder(".")
): Pair<NumberToken, Char?> = when (char) {
    in '0'..'9' -> handleDecimalNumber(traverser, traverser.pop(), builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleDecimalNumber(traverser, traverser.pop(), builder)
        else NumberToken(builder.toString().toDouble()) to char
    'e' -> when (traverser.peek()) {
        '+', '-' -> {
            val symbol = traverser.pop()
            if (traverser.peek()?.isDigit() == true) handleDecimalExponent(
                traverser,
                traverser.pop(),
                builder.append(symbol)
            )
            else error("Unexpected number after symbol in scientific notation")
        }
        in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), builder)
        else -> NumberToken(builder.toString().toDouble()) to char
    }
    else -> NumberToken(builder.toString().toDouble()) to char
}

fun handleDecimalExponent(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder(".")
): Pair<NumberToken, Char?> = when (char) {
    in '0'..'9' -> handleDecimalExponent(traverser, traverser.pop(), builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleDecimalExponent(traverser, traverser.pop(), builder)
        else NumberToken(builder.toString().toDouble()) to char
    else -> NumberToken(builder.toString().toDouble()) to char
}

fun handleBinaryNumber(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<NumberToken, Char?> = when (char) {
    '0', '1' -> handleBinaryNumber(traverser, traverser.pop(), builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleBinaryNumber(traverser, traverser.pop(), builder)
        else NumberToken(builder.toString().toDouble()) to char
    else -> NumberToken(builder.toString().toInt(radix = 2)) to char
}

fun handleHexNumber(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<NumberToken, Char?> = when (char) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> handleHexNumber(traverser, traverser.pop(), builder.append(char))
    '_' ->
        if (traverser.peek()?.isDigit() == true) handleHexNumber(traverser, traverser.pop(), builder)
        else NumberToken(builder.toString().toDouble()) to char
    else -> NumberToken(builder.toString().toInt(radix = 16)) to char
}