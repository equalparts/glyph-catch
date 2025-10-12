package dev.equalparts.glyph_catch.debug

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "debug_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["eventType"])
    ]
)
data class DebugEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val eventType: String,
    val phoneBattery: Int,
    val phoneIsInteractive: Boolean,
    val phoneMinutesOff: Int,
    val phoneMinutesOffOutsideBedtime: Int,
    val queueSize: Int,
    val hasSleepBonus: Boolean,
    val isBedtime: Boolean,
    @ColumnInfo(name = "payloadJson") val payloadJson: String
)

@Dao
interface DebugEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: DebugEvent)

    @Query("SELECT * FROM debug_events ORDER BY timestamp ASC")
    suspend fun getAll(): List<DebugEvent>

    @Query("SELECT COUNT(*) FROM debug_events")
    suspend fun count(): Long

    @Query("SELECT COUNT(*) FROM debug_events")
    fun observeCount(): Flow<Long>

    @Query("DELETE FROM debug_events")
    suspend fun clear()

    @Query(
        "DELETE FROM debug_events WHERE id NOT IN (SELECT id FROM debug_events ORDER BY timestamp DESC LIMIT :maxRows)"
    )
    suspend fun trimTo(maxRows: Int)

    @Query("SELECT MIN(timestamp) FROM debug_events")
    suspend fun firstTimestamp(): Long?

    @Query("SELECT MAX(timestamp) FROM debug_events")
    suspend fun lastTimestamp(): Long?
}
