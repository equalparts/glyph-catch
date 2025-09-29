package dev.equalparts.glyph_catch.screens.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var bedtimeMinutes by remember { mutableIntStateOf(prefs.bedtimeMinutes) }
    var showBedtimePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = hoursPart(bedtimeMinutes),
        initialMinute = minutesPart(bedtimeMinutes),
        is24Hour = DateFormat.is24HourFormat(context)
    )

    val timeFormat = remember { DateFormat.getTimeFormat(context) }
    val wakeMinutes = remember(bedtimeMinutes) { (bedtimeMinutes + 8 * 60) % (24 * 60) }

    if (showBedtimePicker) {
        SleepBedtimePickerDialog(
            timePickerState = timePickerState,
            onDismiss = { showBedtimePicker = false },
            onConfirm = {
                val updated = combinedMinutes(timePickerState.hour, timePickerState.minute)
                bedtimeMinutes = updated
                prefs.bedtimeMinutes = updated
                showBedtimePicker = false
            }
        )
    }

    SleepSettingsContent(
        modifier = modifier,
        bedtimeMinutes = bedtimeMinutes,
        wakeMinutes = wakeMinutes,
        formatMinutes = { minutes -> formatMinutes(minutes, timeFormat) },
        onEditBedtime = {
            timePickerState.hour = hoursPart(bedtimeMinutes)
            timePickerState.minute = minutesPart(bedtimeMinutes)
            showBedtimePicker = true
        }
    )
}

@Composable
private fun SleepSettingsContent(
    modifier: Modifier,
    bedtimeMinutes: Int,
    wakeMinutes: Int,
    formatMinutes: (Int) -> String,
    onEditBedtime: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.verticalScroll(scrollState)) {
        SleepScheduleCard(
            bedtimeMinutes = bedtimeMinutes,
            wakeMinutes = wakeMinutes,
            formatMinutes = formatMinutes,
            onEditBedtime = onEditBedtime
        )
    }
}

@Composable
private fun SleepScheduleCard(
    bedtimeMinutes: Int,
    wakeMinutes: Int,
    formatMinutes: (Int) -> String,
    onEditBedtime: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            SleepScheduleHeader()

            Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

            SleepScheduleRange(
                bedtimeMinutes = bedtimeMinutes,
                wakeMinutes = wakeMinutes,
                formatMinutes = formatMinutes
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

            SleepScheduleDescription()

            Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

            SleepScheduleAction(onEditBedtime = onEditBedtime)
        }
    }
}

@Composable
private fun SleepScheduleHeader() {
    Text(
        text = stringResource(R.string.sleep_section_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SleepScheduleRange(bedtimeMinutes: Int, wakeMinutes: Int, formatMinutes: (Int) -> String) {
    Text(
        text = stringResource(
            R.string.sleep_bedtime_range,
            formatMinutes(bedtimeMinutes),
            formatMinutes(wakeMinutes)
        ),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SleepScheduleDescription() {
    Text(
        text = stringResource(R.string.sleep_section_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SleepScheduleAction(onEditBedtime: () -> Unit) {
    OutlinedButton(onClick = onEditBedtime) {
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(AppSizes.spacingSmall))
        Text(stringResource(R.string.sleep_change_bedtime))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepBedtimePickerDialog(timePickerState: TimePickerState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.sleep_set_bedtime))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.sleep_choose_bedtime)) },
        text = { TimePicker(state = timePickerState) }
    )
}

private fun hoursPart(minutes: Int) = minutes / 60

private fun minutesPart(minutes: Int) = minutes % 60

private fun combinedMinutes(hours: Int, minutes: Int) = hours * 60 + minutes

private fun formatMinutes(minutes: Int, timeFormat: java.text.DateFormat): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hoursPart(minutes))
        set(Calendar.MINUTE, minutesPart(minutes))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return timeFormat.format(calendar.time)
}
