package dev.equalparts.glyph_catch.screens.settings

import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

enum class SettingsDestination(@field:StringRes val titleRes: Int) {
    SectionList(R.string.settings_title),
    Weather(R.string.settings_weather_title),
    Sleep(R.string.settings_sleep_title),
    Debug(R.string.settings_debug_title)
}
