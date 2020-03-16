package com.scientianovateam.palm.registry

import com.scientianovateam.palm.evaluator.Scope
import com.scientianovateam.palm.parser.parse

object Palm {
    @Target(AnnotationTarget.CONSTRUCTOR)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Constructor(val paramNames: Array<String> = [], val defaults: Array<String> = [])

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Name(val name: String)

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Ignore

    fun evaluate(code: String, asType: Class<*>, scope: Scope = Scope()) = parse(code).handleForType(asType, scope)
}

fun String.evaluate(asType: Class<*>, scope: Scope = Scope()) = parse(this).handleForType(asType, scope)