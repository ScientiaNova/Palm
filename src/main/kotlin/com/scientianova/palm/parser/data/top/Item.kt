package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.queries.ItemId

data class Item(val id: ItemId, val kind: ItemKind)

sealed class ItemKind {
    data class Prop(val property: Property) : ItemKind()
    data class Fun(val function: Function) : ItemKind()
    data class Clazz(val clazz: Class) : ItemKind()
    data class Inter(val inter: Interface) : ItemKind()
    data class Obj(val obj: Object) : ItemKind()
    data class TC(val tc: TypeClass) : ItemKind()
    data class Impl(val impl: Implementation) : ItemKind()
    data class Alias(val alias: TypeAlias): ItemKind()
}