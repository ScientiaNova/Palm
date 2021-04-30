package com.scientianova.palm.queries

import com.scientianova.palm.parser.data.top.FileScope
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.util.MultiHashMap
import com.scientianova.palm.util.Path

val fileToItems = HashMap<FileId, List<ItemId>>()
val packageToItems = MultiHashMap<PackageId, ItemId>()

val packageToFiles = HashMap<PackageId, HashMap<String, FileId>>()
val moduleToFiles = MultiHashMap<ModuleId, FileId>()

val superItems = HashMap<ItemId, ItemId>()

val itemTypes = HashMap<ItemId, TypeId>()
val typeItems = HashMap<TypeId, ItemId>()

val packageToPath = HashMap<PackageId, Path>()
val pathToPackage = HashMap<Path, PackageId>()

val itemIdToParsedKind = HashMap<ItemId, ItemKind>()
val fileIdToParsed = HashMap<FileId, FileScope>()

val moduleDeps = HashMap<ModuleId, List<ModuleId>>()