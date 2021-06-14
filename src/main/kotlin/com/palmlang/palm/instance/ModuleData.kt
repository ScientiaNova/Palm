package com.palmlang.palm.instance

import java.io.File

@JvmInline
value class ModuleId(val key: String)

operator fun ModuleId.plus(part: String) = ModuleId(key + part)

class ModuleData(private val revData: RevisionData) {
    internal val map = HashMap<ModuleId, LinkedHashMap<FileName, QueryData<String?>>>()

    operator fun get(id: ModuleId) = map[id]?.values

    operator fun get(id: FileId) = map[id.module]?.get(id.name)

    fun addFolder(folder: File, module: ModuleId, handlers: Map<FileType, (String, ModuleId) -> FileId>) {
        for (file in folder.listFiles() ?: return) {
            val name = file.name
            if (file.isDirectory) addFolder(file, module + name, handlers)
            else {
                val type = FileType(name.substringAfterLast('.'))
                handlers[type]?.let { handler ->
                    val (mod, fileName) = handler(name.substringBeforeLast('.'), module)
                    map.getOrPut(mod, ::linkedMapOf)[fileName] = QueryData(file.readText(), revData.incremented)
                }
            }
        }
    }

    fun addFile(content: String, module: ModuleId, name: FileName, handlers: Map<FileType, (String, ModuleId) -> FileId>) {
        handlers[name.type]?.let { handler ->
            val (mod, fileName) = handler(name.name, module)
            map.getOrPut(mod, ::linkedMapOf)[fileName] = QueryData(content, revData.incremented)
        }
    }

    fun removeFile(module: ModuleId, name: FileName, handlers: Map<FileType, (String, ModuleId) -> FileId>) {
        handlers[name.type]?.let { handler ->
            val (mod, fileName) = handler(name.name, module)
            map.getOrPut(mod, ::linkedMapOf)[fileName] = QueryData(null, revData.incremented)
        }
    }
}