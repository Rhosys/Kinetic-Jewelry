package ch.rhosys.lyra.domain.repository

import ch.rhosys.lyra.domain.model.AppFilter
import kotlinx.coroutines.flow.Flow

interface AppFilterRepository {
    fun observeAll(): Flow<List<AppFilter>>
    suspend fun getByPackageName(packageName: String): AppFilter?
    suspend fun upsert(filter: AppFilter)
    suspend fun delete(packageName: String)
}
