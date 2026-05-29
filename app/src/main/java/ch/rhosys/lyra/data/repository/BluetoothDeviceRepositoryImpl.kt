package ch.rhosys.lyra.data.repository

import ch.rhosys.lyra.data.local.db.dao.BluetoothDeviceDao
import ch.rhosys.lyra.data.local.db.entity.BluetoothDeviceEntity
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
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
