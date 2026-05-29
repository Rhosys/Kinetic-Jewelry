package ch.rhosys.lyra.data.repository

import ch.rhosys.lyra.data.local.db.dao.ContactFilterDao
import ch.rhosys.lyra.data.local.db.entity.ContactFilterEntity
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContactFilterRepositoryImpl @Inject constructor(
    private val dao: ContactFilterDao,
) : ContactFilterRepository {

    override fun observeByApp(packageName: String): Flow<List<ContactFilter>> =
        dao.observeByApp(packageName).map { entities -> entities.map { it.toDomain() } }

    override suspend fun get(packageName: String, groupName: String, contactName: String): ContactFilter? =
        dao.get(packageName, groupName, contactName)?.toDomain()

    override suspend fun upsert(filter: ContactFilter) =
        dao.upsert(ContactFilterEntity.fromDomain(filter))

    override suspend fun delete(packageName: String, groupName: String, contactName: String) =
        dao.delete(packageName, groupName, contactName)
}
