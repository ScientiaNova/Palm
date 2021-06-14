package com.palmlang.palm.instance

class CompilerInstance(
    val revData: RevisionData = RevisionData(),
    val plugins: List<PlugIn>,
    val moduleData: ModuleData = ModuleData(revData)
) {
    val fileHandlers = plugins.mapNotNull { it.handler }.associateBy(FileHandler::type, FileHandler::fn)
}