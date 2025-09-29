package dev.equalparts.glyph_catch.data

/**
 * Tracks the current connection state for the configured weather provider.
 */
enum class WeatherConnectionStatus {
    DISABLED,
    NEVER_CONNECTED,
    CONNECTED,
    FAILED;

    companion object {
        fun fromStored(value: String?): WeatherConnectionStatus = values().firstOrNull { it.name == value } ?: DISABLED
    }
}
