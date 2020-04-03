package com.scientianova.palm.registry

import com.scientianova.palm.parser.IExpression
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.handleExpression
import com.scientianova.palm.tokenizer.tokenize

object Palm {
    @Target(AnnotationTarget.CONSTRUCTOR)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Constructor(val params: Array<String> = [])

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Name(val name: String)

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Ignore

    @JvmStatic
    fun parseExpression(string: String, fileName: String): IExpression {
        val parser = Parser(tokenize(string, fileName), string, fileName)
        return handleExpression(parser, parser.pop()).first.value
    }
}

fun String.parseExpression(fileName: String = "REPL"): IExpression {
    val parser = Parser(tokenize(this, fileName), this, fileName)
    return handleExpression(parser, parser.pop()).first.value
}