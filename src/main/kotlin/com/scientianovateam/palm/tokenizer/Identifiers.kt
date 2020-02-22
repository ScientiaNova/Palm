package com.scientianovateam.palm.tokenizer

fun handleCapitalizedIdentifier(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<CapitalizedIdentifierToken, Char?> =
    if (char?.isJavaIdentifierPart() == true)
        handleCapitalizedIdentifier(traverser, traverser.pop(), builder.append(char))
    else CapitalizedIdentifierToken(builder.toString()) to char

fun handleUncapitalizedIdentifier(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<IToken, Char?> =
    if (char?.isJavaIdentifierPart() == true)
        handleUncapitalizedIdentifier(traverser, traverser.pop(), builder.append(char))
    else handleUncapitalizedString(builder.toString()) to char