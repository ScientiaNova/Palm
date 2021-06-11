package com.palmlang.palm.queries

data class QueryValue<T>(val value: T, val changed: Revision, var checked: Revision = changed)

interface Query<K, V> {
    operator fun get(key: K): QueryValue<V>
}

abstract class AbstractQuery<K, V> : Query<K, V> {
    protected val map = hashMapOf<K, QueryValue<V>>()
}

class InputQuery<K, V> : AbstractQuery<K, V?>() {
    operator fun set(key: K, value: V) {
        map[key] = QueryValue(value, RevisionData.incremented)
    }

    override operator fun get(key: K) = map.computeIfAbsent(key) { QueryValue(null, RevisionData.current) }
}

class SingletonQuery<V>(val compute: () -> V) : Query<Unit, V> {
    private var value = QueryValue(compute(), RevisionData.current)

    override operator fun get(key: Unit) = if (RevisionData.current == value.changed) value else QueryValue(compute(), RevisionData.current).also { value = it }
}

class EagerQuery1<K, V, D1>(val dep: (K) -> QueryValue<D1>, val compute: (D1) -> V) : AbstractQuery<K, V>() {
    override operator fun get(key: K) = run {
        val depValue = dep(key)

        val current = map.getOrElse(key) {
            return QueryValue(compute(depValue.value), RevisionData.current).also { map[key] = it }
        }

        if (depValue.changed.id <= current.checked.id)
            current.apply { checked = RevisionData.current }
        else
            QueryValue(compute(depValue.value), RevisionData.current).also { map[key] = it }
    }
}


class ComputeQuery1<K, V, D1>(val dep: (K) -> QueryValue<D1>, val compute: (D1) -> V) : AbstractQuery<K, V>() {
    override operator fun get(key: K) = run {
        val depValue = dep(key)

        val current = map.getOrElse(key) {
            return QueryValue(compute(depValue.value), RevisionData.current).also { map[key] = it }
        }

        if (depValue.changed.id <= current.checked.id) {
            current.apply { checked = RevisionData.current }
        } else {
            val newValue = compute(depValue.value)
            if (newValue == current.value)
                current.apply { checked = RevisionData.current }
            else
                QueryValue(newValue, RevisionData.current).also { map[key] = it }
        }
    }
}

class ComputeQuery2<K, V, D1, D2>(val dep1: (K) -> QueryValue<D1>, val dep2: (K) -> QueryValue<D2>, val compute: (D1, D2) -> V) : AbstractQuery<K, V>() {
    override operator fun get(key: K) = run {
        val depValue1 = dep1(key)
        val depValue2 = dep2(key)

        val current = map.getOrElse(key) {
            return QueryValue(compute(depValue1.value, depValue2.value), RevisionData.current).also { map[key] = it }
        }

        if (depValue1.changed.id <= current.checked.id && depValue2.changed.id <= current.checked.id) {
            current.apply { checked = RevisionData.current }
        } else {
            val newValue = compute(depValue1.value, depValue2.value)
            if (newValue == current.value)
                current.apply { checked = RevisionData.current }
            else
                QueryValue(newValue, RevisionData.current).also { map[key] = it }
        }
    }
}

class ComputeQuery3<K, V, D1, D2, D3>(val dep1: (K) -> QueryValue<D1>, val dep2: (K) -> QueryValue<D2>, val dep3: (K) -> QueryValue<D3>, val compute: (D1, D2, D3) -> V) : AbstractQuery<K, V>() {
    override operator fun get(key: K) = run {
        val depValue1 = dep1(key)
        val depValue2 = dep2(key)
        val depValue3 = dep3(key)

        val current = map.getOrElse(key) {
            return QueryValue(compute(depValue1.value, depValue2.value, depValue3.value), RevisionData.current).also { map[key] = it }
        }

        if (depValue1.changed.id <= current.checked.id && depValue2.changed.id <= current.checked.id && depValue3.changed.id <= current.checked.id) {
            current.apply { checked = RevisionData.current }
        } else {
            val newValue = compute(depValue1.value, depValue2.value, depValue3.value)
            if (newValue == current.value)
                current.apply { checked = RevisionData.current }
            else
                QueryValue(newValue, RevisionData.current).also { map[key] = it }
        }
    }
}

class ComputeQuery4<K, V, D1, D2, D3, D4>(val dep1: (K) -> QueryValue<D1>, val dep2: (K) -> QueryValue<D2>, val dep3: (K) -> QueryValue<D3>, val dep4: (K) -> QueryValue<D4>, val compute: (D1, D2, D3, D4) -> V) : AbstractQuery<K, V>() {
    override operator fun get(key: K) = run {
        val depValue1 = dep1(key)
        val depValue2 = dep2(key)
        val depValue3 = dep3(key)
        val depValue4 = dep4(key)

        val current = map.getOrElse(key) {
            return QueryValue(compute(depValue1.value, depValue2.value, depValue3.value, depValue4.value), RevisionData.current).also { map[key] = it }
        }

        if (depValue1.changed.id <= current.checked.id && depValue2.changed.id <= current.checked.id && depValue3.changed.id <= current.checked.id && depValue4.changed.id <= current.checked.id) {
            current.apply { checked = RevisionData.current }
        } else {
            val newValue = compute(depValue1.value, depValue2.value, depValue3.value, depValue4.value)
            if (newValue == current.value)
                current.apply { checked = RevisionData.current }
            else
                QueryValue(newValue, RevisionData.current).also { map[key] = it }
        }
    }
}