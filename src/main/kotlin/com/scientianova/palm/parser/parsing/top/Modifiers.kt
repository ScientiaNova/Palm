package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.missingIdentifier
import com.scientianova.palm.errors.unexpectedSymbol
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.isIdentifier
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.AnnotationType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.parseErr
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseCallArgs
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString

fun parseDecModifiers(parser: Parser): List<DecModifier> = recBuildList {
    val current = parser.current
    val modifier = current.decModifier
    when {
        modifier != null -> {
            add(modifier)
            parser.advance()
        }
        current == Token.At -> add(DecModifier.Annotation(parseAnnotation(parser)))
        else -> return this
    }
}

fun parseAnnotation(parser: Parser): Annotation {
    val start = parser.rawLookup(1)

    if (!start.isIdentifier()) {
        parseErr(missingIdentifier, parser.nextPos)
    }

    val type = start.annotationType
    parser.advance()

    val firstIdent = if (type == AnnotationType.Normal) {
        parser.advance().end(start.identString())
    } else {
        parser.advance()
        if (parser.current != Token.Colon) {
            parser.err(unexpectedSymbol(":"))
        }
        parser.advance()
        parseIdent(parser)
    }

    val path = parseAnnotationPath(parser, firstIdent)

    val args = if (parser.current == Token.LParen) {
        parseCallArgs(parser.advance())
    } else {
        emptyList()
    }

    return Annotation(path, args, type)
}

private fun parseAnnotationPath(parser: Parser, firstIdent: PString) = recBuildList(mutableListOf(firstIdent)) {
    if (parser.current == Token.Comma) {
        add(parseIdent(parser.advance()))
    } else {
        this
    }
}

fun parseAnnotations(parser: Parser): List<Annotation> = recBuildList {
    if (parser.current == Token.At) {
        add(parseAnnotation(parser))
    } else {
        return this
    }
}