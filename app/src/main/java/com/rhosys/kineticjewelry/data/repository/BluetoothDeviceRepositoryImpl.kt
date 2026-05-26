package com.rhosys.kineticjewelry.data.repository

import com.rhosys.kineticjewelry.data.local.db.dao.BluetoothDeviceDao
import com.rhosys.kineticjewelry.data.local.db.entity.BluetoothDeviceEntity
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.repository.BluetoothDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BluetoothDeviceRepositoryImpl @Inject constructor(
    private val dao: BluetoothDeviceDao,
) : BluetoothDeviceRepository {

    override fun observeAlertEnabled(): Flow<List<BluetoothDeviceInfo>> =
        dao.observeAlertEnabled().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<BluetoothDeviceInfo> =
        dao.getAll().map { it.toDomain() }

    override suspend fun upsert(device: BluetoothDeviceInfo) =
        dao.upsert(BluetoothDeviceEntity.fromDomain(device))

    override suspend fun delete(address: String) =
        dao.delete(address)
}
