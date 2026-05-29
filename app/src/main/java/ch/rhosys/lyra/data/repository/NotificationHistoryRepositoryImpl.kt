package ch.rhosys.lyra.data.repository

import ch.rhosys.lyra.data.local.db.dao.NotificationHistoryDao
import ch.rhosys.lyra.data.local.db.entity.NotificationHistoryEntity
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import ch.rhosys.lyra.domain.repository.NotificationHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationHistoryRepositoryImpl @Inject constructor(
    private val dao: NotificationHistoryDao,
) : NotificationHistoryRepository {

    private fun cutoff() = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

    override fun observeRecent(): Flow<List<NotificationHistoryEntry>> =
        dao.observeRecent(cutoff()).map { list -> list.map { it.toDomain() } }

    override suspend fun record(entry: NotificationHistoryEntry) {
        dao.deleteOlderThan(cutoff())
        dao.insert(NotificationHistoryEntity.fromDomain(entry))
    }
}
