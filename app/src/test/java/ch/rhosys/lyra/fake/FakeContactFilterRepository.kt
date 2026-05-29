package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeContactFilterRepository : ContactFilterRepository {
    private data class Key(val packageName: String, val groupName: String, val contactName: String)

    private val store = mutableMapOf<Key, ContactFilter>()
    private val _flow = MutableStateFlow<Map<Key, ContactFilter>>(emptyMap())

    override fun observeByApp(packageName: String): Flow<List<ContactFilter>> =
        _flow.map { map -> map.values.filter { it.packageName == packageName } }

    override suspend fun get(packageName: String, groupName: String, contactName: String): ContactFilter? =
        store[Key(packageName, groupName, contactName)]

    override suspend fun upsert(filter: ContactFilter) {
        val key = Key(filter.packageName, filter.groupName, filter.contactName)
        store[key] = filter
        _flow.value = store.toMap()
    }

    override suspend fun delete(packageName: String, groupName: String, contactName: String) {
        store.remove(Key(packageName, groupName, contactName))
        _flow.value = store.toMap()
    }
}
