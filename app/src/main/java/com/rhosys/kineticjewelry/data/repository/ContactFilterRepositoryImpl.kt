package com.rhosys.kineticjewelry.data.repository

import com.rhosys.kineticjewelry.data.local.db.dao.ContactFilterDao
import com.rhosys.kineticjewelry.data.local.db.entity.ContactFilterEntity
import com.rhosys.kineticjewelry.domain.model.ContactFilter
import com.rhosys.kineticjewelry.domain.repository.ContactFilterRepository
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
