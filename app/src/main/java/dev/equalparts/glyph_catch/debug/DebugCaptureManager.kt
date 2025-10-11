package dev.equalparts.glyph_catch.debug

import android.content.Context
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Coordinates opt-in debug logging.
 */
class DebugCaptureManager(
    private val dao: DebugEventDao,
    private val preferences: PreferencesManager,
    private val json: Json = Json { encodeDefaults = true }
) {
    suspend fun log(
        eventType: String,
        snapshot: DebugSnapshot,
        buildPayload: suspend () -> JsonObject = { emptyPayload }
    ) {
        if (!isEnabled()) {
            return
        }

        val payload = runCatching { buildPayload() }.getOrElse { error ->
            buildJsonObject { put("error", JsonPrimitive(error.message ?: "payload_failed")) }
        }

        val entity = DebugEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            batteryPercent = snapshot.batteryPercent,
            isInteractive = snapshot.isInteractive,
            minutesScreenOff = snapshot.minutesScreenOff,
            minutesScreenOffForSpawn = snapshot.minutesScreenOffForSpawn,
            queueSize = snapshot.queueSize,
            sleepMinutesOutside = snapshot.sleepMinutesOutside,
            hasSleepBonus = snapshot.hasSleepBonus,
            isDuringSleepWindow = snapshot.isDuringSleepWindow,
            payloadJson = json.encodeToString(payload)
        )

        withContext(Dispatchers.IO) {
            dao.insert(entity)
            dao.trimTo(MAX_ROWS)
        }
    }

    suspend fun getEvents(): List<DebugEvent> = withContext(Dispatchers.IO) { dao.getAll() }

    fun observeCount(): Flow<Long> = dao.observeCount()

    suspend fun firstTimestamp(): Long? = withContext(Dispatchers.IO) { dao.firstTimestamp() }

    suspend fun lastTimestamp(): Long? = withContext(Dispatchers.IO) { dao.lastTimestamp() }

    suspend fun clear() = withContext(Dispatchers.IO) { dao.clear() }

    fun isEnabled(): Boolean = preferences.debugCaptureEnabled

    fun setEnabled(value: Boolean) {
        preferences.debugCaptureEnabled = value
    }

    companion object {
        private const val MAX_ROWS = 10_000
        private val emptyPayload = JsonObject(emptyMap())

        @Volatile
        private var shared: DebugCaptureManager? = null

        fun shared(context: Context): DebugCaptureManager {
            val cached = shared
            if (cached != null) {
                return cached
            }

            return synchronized(this) {
                val existing = shared
                existing
                    ?: create(context.applicationContext).also { created ->
                        shared = created
                    }
            }
        }

        private fun create(context: Context): DebugCaptureManager {
            val database = PokemonDatabase.getInstance(context)
            val preferences = PreferencesManager(context)
            return DebugCaptureManager(
                dao = database.debugEventDao(),
                preferences = preferences
            )
        }
    }
}
