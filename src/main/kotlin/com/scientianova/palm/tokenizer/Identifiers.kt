package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.MISSING_BACKTICK_ERROR
import com.scientianova.palm.parser.IExpression
import com.scientianova.palm.parser.IOperationPart
import com.scientianova.palm.parser.VirtualCall
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

open class IdentifierToken(override val name: String) : InfixOperatorToken(name, 10), IKeyToken {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, name, listOf(second as IExpression))

    override fun toString() = "IdentifierToken(name=$name)"
}

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