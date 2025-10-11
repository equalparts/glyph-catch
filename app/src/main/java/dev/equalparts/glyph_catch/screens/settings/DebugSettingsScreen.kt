package dev.equalparts.glyph_catch.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.debug.DebugCaptureManager
import dev.equalparts.glyph_catch.debug.DebugExportResult
import dev.equalparts.glyph_catch.debug.DebugLogExporter
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DebugSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val database = remember { PokemonDatabase.getInstance(context) }
    val debugManager = remember { DebugCaptureManager(database.debugEventDao(), prefs) }
    val exporter = remember { DebugLogExporter(context, debugManager) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val debugEnabled by produceState(initialValue = prefs.debugCaptureEnabled, key1 = prefs) {
        prefs.watchDebugCaptureEnabled().collect { value = it }
    }

    var exportInProgress by remember { mutableStateOf(false) }
    var clearInProgress by remember { mutableStateOf(false) }

    val eventStatus by produceState<DebugEventsStatus>(initialValue = DebugEventsStatus.Loading, key1 = debugManager) {
        debugManager.observeCount().collect { count ->
            value = if (count == 0L) {
                DebugEventsStatus.Empty
            } else {
                val first = debugManager.firstTimestamp()
                val last = debugManager.lastTimestamp()
                if (first == null || last == null) {
                    DebugEventsStatus.Empty
                } else {
                    DebugEventsStatus.Populated(first, last)
                }
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        DebugCaptureToggleCard(
            enabled = debugEnabled,
            onToggle = { enabled ->
                debugManager.setEnabled(enabled)
            }
        )

        if (debugEnabled) {
            DebugExportCard(
                onExport = {
                    if (exportInProgress) return@DebugExportCard
                    scope.launch {
                        exportInProgress = true
                        try {
                            when (val result = exporter.export()) {
                                DebugExportResult.Empty -> withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_debug_export_empty),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                is DebugExportResult.Success -> withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.settings_debug_export_success_toast,
                                            result.eventsCount
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    shareDebugLog(context, result.file)
                                }
                            }
                        } catch (_: Throwable) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_debug_export_error),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } finally {
                            exportInProgress = false
                        }
                    }
                },
                exportInProgress = exportInProgress
            )
        }

        DebugEventStatusCard(
            status = eventStatus,
            clearInProgress = clearInProgress,
            onClear = {
                if (clearInProgress) {
                    return@DebugEventStatusCard
                }
                scope.launch {
                    clearInProgress = true
                    try {
                        debugManager.clear()
                    } finally {
                        clearInProgress = false
                    }
                }
            }
        )
    }
}

@Composable
private fun DebugCaptureToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_debug_toggle_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                    Text(
                        text = stringResource(R.string.settings_debug_toggle_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
        }
    }
}

@Composable
private fun DebugEventStatusCard(status: DebugEventsStatus, clearInProgress: Boolean, onClear: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            Text(
                text = stringResource(R.string.settings_debug_events_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            when (status) {
                DebugEventsStatus.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AppSizes.spacingLarge)
                        )
                    }
                }
                DebugEventsStatus.Empty -> {
                    Text(
                        text = stringResource(R.string.settings_debug_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is DebugEventsStatus.Populated -> {
                    DebugEventTimestampRow(
                        label = stringResource(R.string.settings_debug_events_first),
                        timestamp = status.firstMillis
                    )
                    DebugEventTimestampRow(
                        label = stringResource(R.string.settings_debug_events_last),
                        timestamp = status.lastMillis
                    )
                }
            }
            Button(
                onClick = onClear,
                enabled = status is DebugEventsStatus.Populated && !clearInProgress
            ) {
                if (clearInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppSizes.spacingLarge)
                    )
                } else {
                    Text(text = stringResource(R.string.settings_debug_events_clear))
                }
            }
        }
    }
}

@Composable
private fun DebugEventTimestampRow(label: String, timestamp: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatEventTimestamp(timestamp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DebugExportCard(onExport: () -> Unit, exportInProgress: Boolean) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            Text(
                text = stringResource(R.string.settings_debug_export_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_debug_export_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_debug_export_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onExport,
                enabled = !exportInProgress
            ) {
                if (exportInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppSizes.spacingLarge)
                    )
                } else {
                    Text(text = stringResource(R.string.settings_debug_export_button))
                }
            }
        }
    }
}

private sealed class DebugEventsStatus {
    object Loading : DebugEventsStatus()
    object Empty : DebugEventsStatus()
    data class Populated(val firstMillis: Long, val lastMillis: Long) : DebugEventsStatus()
}

private val debugEventTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

private fun formatEventTimestamp(timestamp: Long): String =
    debugEventTimestampFormatter.format(Instant.ofEpochMilli(timestamp))

private fun shareDebugLog(context: Context, file: File) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/x-ndjson"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(
        shareIntent,
        context.getString(R.string.settings_debug_export_share_title)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
