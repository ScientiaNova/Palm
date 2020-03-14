package com.scientianovateam.palm.tokenizer

fun handleIdentifier(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<IToken, Char?> =
    if (char?.isJavaIdentifierPart() == true)
        handleIdentifier(traverser, traverser.pop(), builder.append(char))
    else handleUncapitalizedString(builder.toString()) to char