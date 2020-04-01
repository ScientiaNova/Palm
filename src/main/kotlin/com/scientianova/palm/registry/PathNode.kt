package com.scientianova.palm.registry

sealed class PathNode {
    internal val children = mutableMapOf<String, RegularPathNode>()
    operator fun get(next: String) = children[next]
    operator fun set(name: String, node: RegularPathNode) {
        children[name] = node
    }
}

fun List<String>.toType() = RootPathNode.getType(this)

object RootPathNode : PathNode() {
    fun getType(path: List<String>): IPalmType {
        val iterator = path.iterator()
        return (get(iterator.next()) ?: error("Invalid type path")).getType(iterator)
    }

    internal fun addType(path: List<String>, type: IPalmType): RegularPathNode {
        val iterator = path.iterator()
        val start = iterator.next()
        return addType(iterator, type, get(start) ?: RegularPathNode(this).also { set(start, it) })
    }

    private fun addType(pathIterator: Iterator<String>, type: IPalmType, lastNode: RegularPathNode): RegularPathNode =
        if (pathIterator.hasNext()) {
            val next = pathIterator.next()
            addType(pathIterator, type, lastNode[next] ?: RegularPathNode(lastNode).also { lastNode[next] = it })
        } else lastNode.also { it.type = type }
}

class RegularPathNode(val parent: PathNode) : PathNode() {
    var type: IPalmType? = null
        internal set

    fun getType(pathIterator: Iterator<String>): IPalmType =
        if (pathIterator.hasNext())
            (this[pathIterator.next()] ?: error("Invalid type path")).getType(pathIterator)
        else type ?: error("Invalid type path")
}