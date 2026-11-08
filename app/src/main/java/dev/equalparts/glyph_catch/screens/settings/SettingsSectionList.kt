package dev.equalparts.glyph_catch.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.SaveExport
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsSectionList(modifier: Modifier, onSectionSelected: (SettingsDestination) -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Saves the progress to a file the continued Glyph Catch can import
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = runCatching {
                val content = SaveExport.export(context)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                }
            }.isSuccess
            val message = if (saved) R.string.settings_export_success else R.string.settings_export_error
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        SettingsSectionCard(
            title = stringResource(R.string.settings_weather_title),
            description = stringResource(R.string.settings_section_weather_description),
            onClick = { onSectionSelected(SettingsDestination.Weather) }
        )

        SettingsSectionCard(
            title = stringResource(R.string.settings_sleep_title),
            description = stringResource(R.string.settings_section_sleep_description),
            onClick = { onSectionSelected(SettingsDestination.Sleep) }
        )

        SettingsSectionCard(
            title = stringResource(R.string.settings_graphics_title),
            description = stringResource(R.string.settings_section_graphics_description),
            onClick = { onSectionSelected(SettingsDestination.Graphics) }
        )

        SettingsSectionCard(
            title = stringResource(R.string.settings_debug_title),
            description = stringResource(R.string.settings_section_debug_description),
            onClick = { onSectionSelected(SettingsDestination.Debug) }
        )

        SettingsSectionCard(
            title = stringResource(R.string.settings_export_title),
            description = stringResource(R.string.settings_export_description),
            onClick = { exportLauncher.launch("glyph-catch-${LocalDate.now()}.json") }
        )
    }
}

@Composable
private fun SettingsSectionCard(title: String, description: String, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
