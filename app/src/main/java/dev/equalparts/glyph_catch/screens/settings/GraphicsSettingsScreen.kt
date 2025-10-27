package dev.equalparts.glyph_catch.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager

@Composable
fun GraphicsSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scrollState = rememberScrollState()

    val clockEnabled by produceState(initialValue = prefs.glyphToyClockEnabled, key1 = prefs) {
        prefs.watchGlyphToyClockEnabled().collect { value = it }
    }
    val lowerBrightness by produceState(initialValue = prefs.glyphToyLowerBrightness, key1 = prefs) {
        prefs.watchGlyphToyLowerBrightness().collect { value = it }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        VisualToggleCard(
            title = stringResource(R.string.settings_graphics_clock_title),
            subtitle = stringResource(R.string.settings_graphics_clock_subtitle),
            checked = clockEnabled,
            onCheckedChange = { enabled -> prefs.glyphToyClockEnabled = enabled }
        )

        VisualToggleCard(
            title = stringResource(R.string.settings_graphics_brightness_title),
            subtitle = stringResource(R.string.settings_graphics_brightness_subtitle),
            checked = lowerBrightness,
            onCheckedChange = { enabled -> prefs.glyphToyLowerBrightness = enabled }
        )
    }
}

@Composable
private fun VisualToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
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
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
