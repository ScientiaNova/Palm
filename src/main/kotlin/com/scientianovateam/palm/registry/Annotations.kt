package com.scientianovateam.palm.registry

object Palm {
    @Target(AnnotationTarget.CONSTRUCTOR)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Constructor(val paramNames: Array<String> = [])

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Extension(val extending: String)

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Type

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Name(val name: String)

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Ignore
}