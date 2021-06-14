package com.palmlang.palm.instance

class BasePlugIn : PlugIn {
    override val handler: FileHandler = FileHandler(palmType) { name, mod ->
        if (name == "mod") FileId(mod, FileName("mod", palmType))
        else FileId(mod + name, FileName("", palmType))
    }
}