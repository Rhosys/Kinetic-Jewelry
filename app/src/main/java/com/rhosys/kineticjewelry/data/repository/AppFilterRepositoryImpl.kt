package com.rhosys.kineticjewelry.data.repository

import com.rhosys.kineticjewelry.data.local.db.dao.AppFilterDao
import com.rhosys.kineticjewelry.data.local.db.entity.AppFilterEntity
import com.rhosys.kineticjewelry.domain.model.AppFilter
import com.rhosys.kineticjewelry.domain.repository.AppFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppFilterRepositoryImpl @Inject constructor(
    private val dao: AppFilterDao,
) : AppFilterRepository {

    override fun observeAll(): Flow<List<AppFilter>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getByPackageName(packageName: String): AppFilter? =
        dao.getByPackageName(packageName)?.toDomain()

    override suspend fun upsert(filter: AppFilter) =
        dao.upsert(AppFilterEntity.fromDomain(filter))

    override suspend fun delete(packageName: String) =
        dao.delete(packageName)
}
