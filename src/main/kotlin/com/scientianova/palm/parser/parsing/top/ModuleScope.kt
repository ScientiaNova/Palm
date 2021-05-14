package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.implIdent
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.SharedParseData
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.ModuleScope
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.types.parseImpl
import com.scientianova.palm.queries.*
import com.scientianova.palm.util.recBuildList
import java.io.File

fun parseCrate(root: File) = SharedParseData(mutableListOf()).also { it.parseDir(null, root) }

fun SharedParseData.parseDir(id: ModuleId?, dir: File) {
    val (dirs, files) = (dir.listFiles() ?: return).partition { it.isDirectory }
    val submodules = hashMapOf<String, ModuleId>()

    dirs.forEach { subDir ->
        val subId = ModuleId()
        val name = subDir.name

        moduleNames[subId] = name
        parentModule[subId] = id
        submodules[name] = subId

        parseDir(subId, subDir)
    }

    files.forEach { file ->
        val fullName = file.name
        val extension = fullName.substringAfterLast('.')
        if (extension != "palm") return@forEach
        val name = fullName.dropLast(extension.length + 1)

        if (name == "mod") {
            when (id) {
                null -> err("The root cannot contain items", file)
                !in moduleIdToParsed -> fileParser(file).parseFile(id)
                else -> err("The same module cannot have 2 module files", file)
            }
        } else {
            val subId = ModuleId()
            moduleNames[subId] = name
            parentModule[subId] = id
            submodules[name] = subId
            fileParser(file).parseFile(subId)
        }
    }

    moduleToSubmodules[id] = submodules
}

fun Parser.parseFile(id: ModuleId) {
    val annotations = parseFileAnnotations()
    val imports = parseImports()
    moduleToItems[id] = parseStatements()
    moduleIdToParsed[id] = ModuleScope(annotations, imports)
}

fun Parser.parseInitializer(): ItemId = registerParsedItem {
    ItemKind.Initializer(advance().requireScope())
}

private fun Parser.parseStatements() = recBuildList<ItemId> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        initIdent -> add(parseInitializer())
        implIdent -> add(advance().parseImpl())
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> add(parseInitializer())
                else -> parseItem(modifiers)?.let(::add)
            }
        }
    }
}