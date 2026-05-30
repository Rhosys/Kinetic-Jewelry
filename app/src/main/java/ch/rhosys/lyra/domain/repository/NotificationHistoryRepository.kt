package ch.rhosys.lyra.domain.repository

import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import kotlinx.coroutines.flow.Flow

interface NotificationHistoryRepository {
    fun observeRecent(): Flow<List<NotificationHistoryEntry>>
    suspend fun record(entry: NotificationHistoryEntry)
}
