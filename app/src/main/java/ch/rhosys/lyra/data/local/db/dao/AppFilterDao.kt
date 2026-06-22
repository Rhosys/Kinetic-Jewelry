package ch.rhosys.lyra.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ch.rhosys.lyra.data.local.db.entity.AppFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppFilterDao {
    @Query("SELECT * FROM app_filters ORDER BY appLabel COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppFilterEntity>>

    @Query("SELECT * FROM app_filters WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): AppFilterEntity?

    @Upsert
    suspend fun upsert(entity: AppFilterEntity)

    @Query("DELETE FROM app_filters WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
