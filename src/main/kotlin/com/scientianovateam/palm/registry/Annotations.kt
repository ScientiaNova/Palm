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
    annotation class Type(val registryPath: String)

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Name(val name: String)

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Ignore
}