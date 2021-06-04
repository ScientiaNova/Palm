package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map
import com.scientianova.palm.util.recBuildList

fun Parser.parsePattern(): Pattern = when (val token = current) {
    Token.Wildcard -> Pattern.Wildcard.also { advance() }
    Token.Let -> Pattern.Dec(advance().requireDecPattern())
    Token.Is -> Pattern.Type(advance().requireType(), false, parseDestructuring())
    Token.ExclamationMark -> when (next) {
        Token.Is -> Pattern.Type(advance().advance().requireType(), true, null)
        else -> Pattern.Expr(requireExpr())
    }
    is Token.Parens -> when (next) {
        Token.Arrow, Token.Pipe, Token.When -> parseTuplePattern(token.tokens)
        else -> Pattern.Expr(requireExpr())
    }
    else -> Pattern.Expr(requireExpr())
}

private fun Parser.parseTuplePatternBody(): List<Pattern> = recBuildList {
    if (current == Token.End)
        return this

    add(parsePattern())
    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

private fun Parser.parseTuplePattern(tokens: List<PToken>): Pattern =
    Pattern.Tuple(parenthesizedOf(tokens).parseTuplePatternBody().also { advance() })

private fun Parser.parseObjectPatternBody(): List<Pair<PString, Pattern>> = recBuildList {
    when (current) {
        Token.End -> return this
        Token.Let -> if (advance().current == Token.Mut) {
            val mutStart = pos
            val ident = advance().parseIdent()
            add(ident to Pattern.Dec(
                DecPattern.Mut(ident.map(DecPattern::Name)).at(mutStart, ident.next)
            ))
        } else {
            val ident = parseIdent()
            add(ident to Pattern.Dec(ident.map(DecPattern::Name)))
        }
        else -> {
            val ident = parseIdent()
            if (current == Token.Colon) advance() else err("Missing colon")
            add(ident to parsePattern())
        }
    }

    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

fun Parser.parseDestructuring() = when (val token = current) {
    is Token.Parens -> Destructuring.Components(
        parenthesizedOf(token.tokens).parseTuplePatternBody().also { advance() })
    is Token.Braces -> Destructuring.Object(parenthesizedOf(token.tokens).parseObjectPatternBody()).also { advance() }
    else -> null
}