package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.MISSING_BACKTICK_ERROR
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

fun handleIdentifier(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos = traverser.lastPos,
    builder: StringBuilder = StringBuilder()
): Pair<Positioned<IToken>, Char?> =
    if (char?.isJavaIdentifierPart() == true)
        handleIdentifier(traverser, traverser.pop(), startPos, builder.append(char))
    else handleUncapitalizedString(builder.toString()) on startPos..traverser.lastPos to char

fun handleBacktickedIdentifier(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos = traverser.lastPos,
    builder: StringBuilder = StringBuilder()
): Pair<Positioned<IToken>, Char?> = when (char) {
    null, '\n' -> traverser.error(MISSING_BACKTICK_ERROR, traverser.lastPos)
    '`' -> IdentifierToken(builder.toString()) on startPos..traverser.lastPos to traverser.pop()
    else -> handleBacktickedIdentifier(traverser, traverser.pop(), startPos, builder.append(char))
}