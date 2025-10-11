package dev.equalparts.glyph_catch.debug

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class DebugLogExporter(
    private val context: Context,
    private val debugCaptureManager: DebugCaptureManager,
    private val json: Json = Json { prettyPrint = true }
) {
    suspend fun export(): DebugExportResult = withContext(Dispatchers.IO) {
        val events = debugCaptureManager.getEvents()
        if (events.isEmpty()) {
            return@withContext DebugExportResult.Empty
        }

        val targetDir = File(context.cacheDir, EXPORT_DIR_NAME).apply { mkdirs() }
        val outputFile = File(targetDir, EVENTS_FILENAME)

        writeEventsNdjson(events, outputFile)

        debugCaptureManager.setEnabled(false)

        DebugExportResult.Success(outputFile, events.size)
    }

    private fun writeEventsNdjson(events: List<DebugEvent>, file: File) {
        file.bufferedWriter().use { writer ->
            for (event in events) {
                val payloadElement = runCatching { json.parseToJsonElement(event.payloadJson) }
                    .getOrElse { JsonNull }
                val line = buildJsonObject {
                    put("id", JsonPrimitive(event.id))
                    put("timestamp", JsonPrimitive(event.timestamp))
                    put("timestampIso", JsonPrimitive(formatIso(event.timestamp)))
                    put("eventType", JsonPrimitive(event.eventType))
                    put("batteryPercent", JsonPrimitive(event.batteryPercent))
                    put("isInteractive", JsonPrimitive(event.isInteractive))
                    put("minutesScreenOff", JsonPrimitive(event.minutesScreenOff))
                    put("minutesScreenOffForSpawn", JsonPrimitive(event.minutesScreenOffForSpawn))
                    put("queueSize", JsonPrimitive(event.queueSize))
                    put("sleepMinutesOutside", JsonPrimitive(event.sleepMinutesOutside))
                    put("hasSleepBonus", JsonPrimitive(event.hasSleepBonus))
                    put("isDuringSleepWindow", JsonPrimitive(event.isDuringSleepWindow))
                    put("payload", payloadElement)
                }
                writer.write(json.encodeToString(line))
                writer.newLine()
            }
        }
    }

    private fun formatIso(timestamp: Long): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        .withLocale(Locale.US)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

    companion object {
        private const val EXPORT_DIR_NAME = "debug-exports"
        private const val EVENTS_FILENAME = "glyph_debug_events.ndjson"
    }
}

sealed class DebugExportResult {
    data class Success(val file: File, val eventsCount: Int) : DebugExportResult()
    object Empty : DebugExportResult()
}
