package com.scientianovateam.palm.registry

object Palm {
    @Target(AnnotationTarget.CONSTRUCTOR)
    annotation class Constructor(val paramNames: Array<String> = [])

    @Target(AnnotationTarget.CLASS)
    annotation class Extension(val extending: String)

    @Target(AnnotationTarget.CLASS)
    annotation class Type(val registryPath: String)

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
    annotation class Name(val name: String)

    @Target(AnnotationTarget.FUNCTION)
    annotation class Operator

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
    annotation class Setter

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER)
    annotation class Getter

    @Target(AnnotationTarget.FUNCTION)
    annotation class AutoCaster
}