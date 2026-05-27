package com.rhosys.kineticjewelry.domain.repository

import com.rhosys.kineticjewelry.domain.model.ContactFilter
import kotlinx.coroutines.flow.Flow

interface ContactFilterRepository {
    fun observeByApp(packageName: String): Flow<List<ContactFilter>>
    suspend fun get(packageName: String, groupName: String, contactName: String): ContactFilter?
    suspend fun upsert(filter: ContactFilter)
    suspend fun delete(packageName: String, groupName: String, contactName: String)
}
