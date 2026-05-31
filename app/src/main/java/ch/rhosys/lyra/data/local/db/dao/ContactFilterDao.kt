package ch.rhosys.lyra.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ch.rhosys.lyra.data.local.db.entity.ContactFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactFilterDao {
    @Query("SELECT * FROM contact_filters WHERE packageName = :packageName ORDER BY contactName ASC")
    fun observeByApp(packageName: String): Flow<List<ContactFilterEntity>>

    @Query("SELECT * FROM contact_filters WHERE packageName = :packageName ORDER BY contactName ASC")
    suspend fun getAllByApp(packageName: String): List<ContactFilterEntity>

    @Query("""
        SELECT * FROM contact_filters
        WHERE packageName = :packageName AND groupName = :groupName AND contactName = :contactName
        LIMIT 1
    """)
    suspend fun get(packageName: String, groupName: String, contactName: String): ContactFilterEntity?

    @Upsert
    suspend fun upsert(entity: ContactFilterEntity)

    @Query("""
        DELETE FROM contact_filters
        WHERE packageName = :packageName AND groupName = :groupName AND contactName = :contactName
    """)
    suspend fun delete(packageName: String, groupName: String, contactName: String)
}
