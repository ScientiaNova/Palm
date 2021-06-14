package com.palmlang.palm.instance

data class QueryData<T>(val value: T, val changed: Revision, var checked: Revision = changed)

interface Query<K, V> {
    operator fun get(key: K, rev: Revision): QueryData<V>
}

class SingletonQuery<V>(val compute: () -> V) : Query<Unit, V> {
    private var value = QueryData(compute(), Revision(0))

    override operator fun get(key: Unit, rev: Revision): QueryData<V> =
        if (rev == value.changed) value else QueryData(compute(), rev).also { value = it }
}

abstract class AbstractQuery<K, V> : Query<K, V> {
    protected val map = hashMapOf<K, QueryData<V>>()
}

class EagerQuery1<K, V, D1>(
    val dep: (K) -> QueryData<D1>,
    val compute: (D1) -> V
) : AbstractQuery<K, V>() {
    override operator fun get(key: K, rev: Revision): QueryData<V> = run {
        val depValue = dep(key)

        val current = map.getOrElse(key) {
            return QueryData(compute(depValue.value), rev).also { map[key] = it }
        }

        when {
            current.checked.id >= rev.id -> current
            depValue.changed.id <= current.checked.id ->
                current.apply { checked = rev }
            else ->
                QueryData(compute(depValue.value), rev).also { map[key] = it }
        }
    }
}


class ComputeQuery1<K, V, D1>(
    val dep: (K) -> QueryData<D1>,
    val compute: (D1) -> V
) : AbstractQuery<K, V>() {
    override operator fun get(key: K, rev: Revision) = run {
        val depValue = dep(key)

        val current = map.getOrElse(key) {
            return QueryData(compute(depValue.value), rev).also { map[key] = it }
        }

        when {
            current.checked.id >= rev.id -> current
            depValue.changed.id <= current.checked.id ->
                current.apply { checked = rev }
            else -> {
                val newValue = compute(depValue.value)
                if (newValue == current.value)
                    current.apply { checked = rev }
                else
                    QueryData(newValue, rev).also { map[key] = it }
            }
        }
    }
}

class ComputeQuery2<K, V, D1, D2>(
    val dep1: (K) -> QueryData<D1>,
    val dep2: (K) -> QueryData<D2>,
    val compute: (D1, D2) -> V
) : AbstractQuery<K, V>() {
    override operator fun get(key: K, rev: Revision): QueryData<V> = run {
        val depValue1 = dep1(key)
        val depValue2 = dep2(key)

        val current = map.getOrElse(key) {
            return QueryData(compute(depValue1.value, depValue2.value), rev).also { map[key] = it }
        }

        when {
            current.checked.id >= rev.id -> current
            depValue1.changed.id <= current.checked.id && depValue2.changed.id <= current.checked.id ->
                current.apply { checked = rev }
            else -> {
                val newValue = compute(depValue1.value, depValue2.value)
                if (newValue == current.value)
                    current.apply { checked = rev }
                else
                    QueryData(newValue, rev).also { map[key] = it }
            }
        }
    }
}

class ComputeQuery3<K, V, D1, D2, D3>(
    val dep1: (K) -> QueryData<D1>,
    val dep2: (K) -> QueryData<D2>,
    val dep3: (K) -> QueryData<D3>,
    val compute: (D1, D2, D3) -> V
) : AbstractQuery<K, V>() {
    override operator fun get(key: K, rev: Revision): QueryData<V> = run {
        val depValue1 = dep1(key)
        val depValue2 = dep2(key)
        val depValue3 = dep3(key)

        val current = map.getOrElse(key) {
            return QueryData(compute(depValue1.value, depValue2.value, depValue3.value), rev).also { map[key] = it }
        }

        when {
            current.checked.id >= rev.id -> current
            depValue1.changed.id <= current.checked.id && depValue2.changed.id <= current.checked.id && depValue3.changed.id <= current.checked.id ->
                current.apply { checked = rev }
            else -> {
                val newValue = compute(depValue1.value, depValue2.value, depValue3.value)
                if (newValue == current.value)
                    current.apply { checked = rev }
                else
                    QueryData(newValue, rev).also { map[key] = it }
            }
        }
    }
}

class ComputeQuery4<K, V, D1, D2, D3, D4>(
    val dep1: (K) -> QueryData<D1>,
    val dep2: (K) -> QueryData<D2>,
    val dep3: (K) -> QueryData<D3>,
    val dep4: (K) -> QueryData<D4>,
    val compute: (D1, D2, D3, D4) -> V
) : AbstractQuery<K, V>() {
    override operator fun get(key: K, rev: Revision): QueryData<V> = run {
        val depValue1 = dep1(key)
        val depValue2 = dep2(key)
        val depValue3 = dep3(key)
        val depValue4 = dep4(key)

        val current = map.getOrElse(key) {
            return QueryData(compute(depValue1.value, depValue2.value, depValue3.value, depValue4.value), rev)
                .also { map[key] = it }
        }

        when {
            current.checked.id >= rev.id -> current
            depValue1.changed.id <= current.checked.id && depValue2.changed.id <= current.checked.id && depValue3.changed.id <= current.checked.id && depValue4.changed.id <= current.checked.id ->
                current.apply { checked = rev }
            else -> {
                val newValue = compute(depValue1.value, depValue2.value, depValue3.value, depValue4.value)
                if (newValue == current.value)
                    current.apply { checked = rev }
                else
                    QueryData(newValue, rev).also { map[key] = it }
            }
        }
    }
}