package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

data class TokenData(val token: Token, val startPos: StringPos, val nextPos: StringPos)

val TokenData.lastPos get() = nextPos - 1
fun Token.data(startPos: StringPos, nextPos: StringPos = startPos + 1) = TokenData(this, startPos, nextPos)