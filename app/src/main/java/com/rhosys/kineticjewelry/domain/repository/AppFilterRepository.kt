package com.rhosys.kineticjewelry.domain.repository

import com.rhosys.kineticjewelry.domain.model.AppFilter
import kotlinx.coroutines.flow.Flow

interface AppFilterRepository {
    fun observeAll(): Flow<List<AppFilter>>
    suspend fun getByPackageName(packageName: String): AppFilter?
    suspend fun upsert(filter: AppFilter)
    suspend fun delete(packageName: String)
}
