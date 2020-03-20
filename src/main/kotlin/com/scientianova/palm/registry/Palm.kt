package com.scientianova.palm.registry

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.parser.IExpression
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.handleExpression
import com.scientianova.palm.parser.parse
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

    @JvmStatic
    fun evaluate(code: String, asType: Class<*>, fileName: String, scope: Scope = Scope()) =
        parse(code, fileName).handleForType(asType, scope)
}

fun String.evaluate(asType: Class<*>, fileName: String = "REPL", scope: Scope = Scope()) =
    parse(this, fileName).handleForType(asType, scope)

fun String.parseExpression(fileName: String = "REPL"): IExpression {
    val parser = Parser(tokenize(this, fileName), this, fileName)
    return handleExpression(parser, parser.pop()).first.value
}