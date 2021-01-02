package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.fileIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.top.AnnotationType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseCallArgs
import com.scientianova.palm.util.recBuildList

fun identToDecMod(string: String) = when (string) {
    "inline" -> DecModifier.Inline
    "override" -> DecModifier.Inline
    "open" -> DecModifier.Open
    "final" -> DecModifier.Final
    "public" -> DecModifier.Public
    "protected" -> DecModifier.Protected
    "internal" -> DecModifier.Internal
    "private" -> DecModifier.Private
    "abstract" -> DecModifier.Abstract
    "data" -> DecModifier.Data
    "annotation" -> DecModifier.Ann
    "enum" -> DecModifier.Enum
    "sealed" -> DecModifier.Sealed
    "const" -> DecModifier.Const
    "lateinit" -> DecModifier.Lateinit
    "noinline" -> DecModifier.NoInline
    "crossinline" -> DecModifier.CrossInline
    else -> null
}

fun Parser.parseDecModifiers(): List<DecModifier> = recBuildList {
    when (val token = current) {
        is Token.Ident -> if (token.backticked) return this else when (next) {
            Token.Colon, Token.End, Token.Comma, Token.Assign -> return this
            else -> add(identToDecMod(token.name) ?: return this).also { advance() }
        }
        Token.At -> parseAnnotation()?.let { add(DecModifier.Annotation(it)) } ?: return this
        else -> return this
    }
}

private fun Parser.parseAnnotationType(ident: String): AnnotationType = if (next == Token.Colon) {
    advance().advance()
    when (ident) {
        "file" -> AnnotationType.File
        "delegate" -> AnnotationType.Delegate
        "get" -> AnnotationType.Get
        "set" -> AnnotationType.Set
        "init" -> AnnotationType.Init
        "property" -> AnnotationType.Property
        "field" -> AnnotationType.Field
        "param" -> AnnotationType.Param
        "setParam" -> AnnotationType.SetParam
        else -> {
            err("Unknown annotation type")
            AnnotationType.Normal
        }
    }
} else AnnotationType.Normal

fun Parser.parseAnnotation(): Annotation? {
    val start = rawLookup(1)
    if (start is Token.Ident) advance() else {
        err("Missing identifier")
        return null
    }

    val type = if (start.backticked) AnnotationType.Normal else parseAnnotationType(start.name)
    val path = parseAnnotationPath()
    val args = inParensOrEmpty(Parser::parseCallArgs)

    return Annotation(path, args, type)
}

private fun Parser.parseAnnotationPath() = recBuildList(mutableListOf(parseIdent())) {
    if (current == Token.Dot) {
        add(advance().parseIdent())
    } else {
        return this
    }
}

fun Parser.parseFileAnnotations(): List<Annotation> = recBuildList {
    val startIdent = index
    if (current == Token.At && rawLookup(1) === fileIdent && advance().advance().current == Token.Colon) {
        val path = advance().parseAnnotationPath()
        val args = inParensOrEmpty(Parser::parseCallArgs).also { advance() }
        add(Annotation(path, args, AnnotationType.File))
    } else {
        index = startIdent
        return this
    }
}