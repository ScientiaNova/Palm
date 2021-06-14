package com.palmlang.palm.instance

data class FileType(val extension: String)
 data class FileId(val module: ModuleId, val name: FileName)
data class FileName(val name: String, val type: FileType)

val palmType = FileType("palm")
val javaType = FileType("palm")

data class FileHandler(val type: FileType, val fn: (String, ModuleId) -> FileId)

data class ItemId(val file: FileId, val uniqueId: String)

data class ScopeId(val file: FileId, val uniqueId: String)