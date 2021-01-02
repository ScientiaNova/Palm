package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.map
import com.scientianova.palm.util.recBuildList

fun Parser.parsePattern(): Pattern = when (val token = current) {
    Token.Wildcard -> Pattern.Wildcard.also { advance() }
    Token.Var -> Pattern.Dec(advance().requireDecPattern(), true)
    Token.Val -> Pattern.Dec(advance().requireDecPattern(), false)
    Token.Var -> Pattern.Dec(advance().requireDecPattern(), true)
    Token.Is -> Pattern.Type(advance().requireType(), false, parseDestructuring())
    Token.In -> Pattern.In(advance().requireBinOps(), false)
    Token.ExclamationMark -> when (next) {
        Token.Is -> Pattern.Type(advance().advance().requireType(), true, null)
        Token.In -> Pattern.In(advance().advance().requireBinOps(), true)
        else -> Pattern.Expr(requireBinOps())
    }
    is Token.Parens -> when (next) {
        Token.Arrow, Token.If, Token.When -> parseTuplePattern(token.tokens)
        else -> Pattern.Expr(requireBinOps())
    }
    else -> Pattern.Expr(requireBinOps())
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
        Token.Val -> {
            val ident = advance().parseIdent()
            add(ident to Pattern.Dec(ident.map(DecPattern::Name), false))
        }
        Token.Var -> {
            val ident = advance().parseIdent()
            add(ident to Pattern.Dec(ident.map(DecPattern::Name), true))
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