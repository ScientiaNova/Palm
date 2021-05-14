package com.scientianova.palm.queries

import com.scientianova.palm.parser.data.top.ModuleScope
import com.scientianova.palm.parser.data.top.ItemKind

val moduleToItems = HashMap<ModuleId, List<ItemId>>()
val itemToModule = HashMap<ItemId, ModuleId>()
val superItems = HashMap<ItemId, ItemId>()

val itemTypes = HashMap<ItemId, TypeId>()
val typeItems = HashMap<TypeId, ItemId>()

val itemIdToParsedKind = HashMap<ItemId, ItemKind>()
val moduleIdToParsed = HashMap<ModuleId, ModuleScope>()

val parentModule = HashMap<ModuleId, ModuleId?>()
val moduleToSubmodules = HashMap<ModuleId?, HashMap<String, ModuleId>>()
val moduleNames = HashMap<ModuleId, String>()