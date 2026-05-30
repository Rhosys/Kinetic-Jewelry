package ch.rhosys.lyra.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ch.rhosys.lyra.data.local.db.entity.NotificationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Insert
    suspend fun insert(entity: NotificationHistoryEntity)

    @Query("SELECT * FROM notification_history WHERE postedAt >= :cutoff ORDER BY postedAt DESC")
    fun observeRecent(cutoff: Long): Flow<List<NotificationHistoryEntity>>

    @Query("DELETE FROM notification_history WHERE postedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
