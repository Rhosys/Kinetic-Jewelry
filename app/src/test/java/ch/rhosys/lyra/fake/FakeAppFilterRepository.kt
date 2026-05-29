package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppFilterRepository : AppFilterRepository {
    private val store = mutableMapOf<String, AppFilter>()
    private val _flow = MutableStateFlow<Map<String, AppFilter>>(emptyMap())

    override fun observeAll(): Flow<List<AppFilter>> = _flow.map { it.values.toList() }

    override suspend fun getByPackageName(packageName: String): AppFilter? = store[packageName]

    override suspend fun upsert(filter: AppFilter) {
        store[filter.packageName] = filter
        _flow.value = store.toMap()
    }

    override suspend fun delete(packageName: String) {
        store.remove(packageName)
        _flow.value = store.toMap()
    }
}
