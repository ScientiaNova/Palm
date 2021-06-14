package com.palmlang.palm.instance

class JavaPlugIn : PlugIn {
    override val handler: FileHandler = FileHandler(javaType) { name, mod ->
        FileId(mod, FileName(name, javaType))
    }
}