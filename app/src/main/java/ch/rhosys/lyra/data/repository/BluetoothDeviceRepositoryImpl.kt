package ch.rhosys.lyra.data.repository

import ch.rhosys.lyra.data.local.db.dao.BluetoothDeviceDao
import ch.rhosys.lyra.data.local.db.entity.BluetoothDeviceEntity
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val AUTO_DISABLE_COUNT = 2
private const val AUTO_DISABLE_DURATION_MS = 60 * 60 * 1_000L

class BluetoothDeviceRepositoryImpl
    @Inject
    constructor(
        private val dao: BluetoothDeviceDao,
    ) : BluetoothDeviceRepository {
        override fun observeAlertEnabled(): Flow<List<BluetoothDeviceInfo>> =
            dao.observeAlertEnabled().map { entities -> entities.map { it.toDomain() } }

        override suspend fun getAll(): List<BluetoothDeviceInfo> = dao.getAll().map { it.toDomain() }

        override suspend fun upsert(device: BluetoothDeviceInfo) = dao.upsert(BluetoothDeviceEntity.fromDomain(device))

        override suspend fun delete(address: String) = dao.delete(address)

        override suspend fun recordSuccess(address: String) {
            dao.resetConsecutiveTimeouts(address)
            dao.setDisabledUntil(address, null)
            dao.setLastSuccessAt(address, System.currentTimeMillis())
        }

        override suspend fun recordFailure(address: String) {
            dao.incrementConsecutiveTimeouts(address)
            val count = dao.getConsecutiveTimeouts(address)
            if (count >= AUTO_DISABLE_COUNT) {
                dao.setDisabledUntil(address, System.currentTimeMillis() + AUTO_DISABLE_DURATION_MS)
            }
        }

        override suspend fun setDisabledUntil(
            address: String,
            disabledUntil: Long?,
        ) = dao.setDisabledUntil(address, disabledUntil)
    }
