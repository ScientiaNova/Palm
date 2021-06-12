package com.palmlang.palm.parser.expressions

import com.palmlang.palm.ast.expressions.DecPattern
import com.palmlang.palm.ast.expressions.Destructuring
import com.palmlang.palm.ast.expressions.Pattern
import com.palmlang.palm.lexer.PToken
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.at
import com.palmlang.palm.util.map
import com.palmlang.palm.util.recBuildList

fun Parser.parsePattern(): Pattern = when (val token = current) {
    Token.Wildcard -> Pattern.Wildcard.also { advance() }
    Token.Let -> Pattern.Dec(advance().requireDecPattern())
    Token.Is -> Pattern.Type(advance().requireType(), false, parseDestructuring())
    Token.ExclamationMark -> when (next) {
        Token.Is -> Pattern.Type(advance().advance().requireType(), true, null)
        else -> Pattern.Expr(requireExpr())
    }
    is Token.Parens -> when (next) {
        Token.Arrow, Token.Pipe, Token.When -> parseParenthesizedPattern(token.tokens)
        else -> Pattern.Expr(requireExpr())
    }
    else -> Pattern.Expr(requireExpr())
}

private fun Parser.parseParenthesizedPattern(tokens: List<PToken>): Pattern =
    parenthesizedOf(tokens).parseCommaSeparated(Parser::parsePattern) { patterns, trailing ->
        if (patterns.size == 1 && !trailing) Pattern.Parenthesized(patterns[0])
        else Pattern.Tuple(patterns)
    }.also { advance() }

private fun Parser.parseObjectPatternBody(): List<Pair<PString, Pattern>> = recBuildList {
    when (current) {
        Token.End -> return this
        Token.Let -> if (advance().current == Token.Mut) {
            val mutStart = pos
            val ident = advance().parseIdent()
            add(
                ident to Pattern.Dec(
                    DecPattern.Mut(ident.map(DecPattern::Name)).at(mutStart, ident.next)
                )
            )
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
    is Token.Parens -> parenthesizedOf(token.tokens).parseCommaSeparated(Parser::parsePattern) { components, _ ->
        Destructuring.Components(components)
    }.also { advance() }
    is Token.Braces -> Destructuring.Object(parenthesizedOf(token.tokens).parseObjectPatternBody()).also { advance() }
    else -> null
}