package com.scientianovateam.palm.registry

import com.scientianovateam.palm.parser.BinaryOperation
import com.scientianovateam.palm.parser.IType
import com.scientianovateam.palm.parser.MultiOperation
import com.scientianovateam.palm.parser.UnaryOperation
import com.scientianovateam.palm.util.HashTable
import com.scientianovateam.palm.util.MultiHashMap
import java.lang.reflect.Method

class PalmType(val constructor: PalmConstructor?) {
    val setters = HashTable<String, String, PalmSetter>()
    val getters = hashMapOf<String, PalmGetter>()
    val autoCaster = hashMapOf<IType, Method>()
    val operators = Operators()
}

class Operators {
    val unary = hashMapOf<UnaryOperation, Method>()
    val binary = HashTable<BinaryOperation, IType, PalmBinaryOperator>()
    val multi = MultiHashMap<MultiOperation, PalmMultiOperator>()
}

class PalmConstructor(val method: Method, val params: Array<String>, val paramTypes: Array<IType>)
class PalmGetter(val method: Method, val paramTypes: Array<IType>, val returnType: IType)
class PalmSetter(val method: Method, val valueType: IType)
class PalmBinaryOperator(val method: Method, val returnType: IType)
class PalmMultiOperator(val method: Method, val paramTypes: Array<IType>, val returnType: IType)