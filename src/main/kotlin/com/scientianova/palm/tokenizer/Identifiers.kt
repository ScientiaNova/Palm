package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

tailrec fun handleIdentifier(
    traverser: StringTraverser,
    char: Char?,
    capitalized: Boolean,
    list: TokenList,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PToken, Char?> =
    if (char?.isJavaIdentifierPart() == true)
        handleIdentifier(traverser, traverser.pop(), capitalized, list, startPos, builder.append(char))
    else handleUncapitalizedString(builder.toString()) on startPos..traverser.lastPos.shift(-1) to char