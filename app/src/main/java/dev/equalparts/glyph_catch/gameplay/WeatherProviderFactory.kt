package dev.equalparts.glyph_catch.gameplay

import android.content.Context
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.WeatherConfig
import dev.equalparts.glyph_catch.data.WeatherConnectionStatus
import dev.equalparts.glyph_catch.gameplay.spawner.WeatherProvider

/**
 * Factory for creating the appropriate weather provider based on user settings.
 */
object WeatherProviderFactory {

    fun create(context: Context): WeatherProvider = PreferencesManager(context).let { prefs ->
        create(prefs, prefs.getWeatherConfig())
    }

    fun create(prefs: PreferencesManager, config: WeatherConfig = prefs.getWeatherConfig()): WeatherProvider =
        if (config.useOpenWeather) {
            if (config.isConfigured) {
                OpenWeatherMapProvider(
                    prefs = prefs,
                    apiKey = config.apiKey!!,
                    latitude = config.latitude.toDouble(),
                    longitude = config.longitude.toDouble()
                )
            } else {
                prefs.weatherConnectionStatus = WeatherConnectionStatus.NEVER_CONNECTED
                prefs.weatherLastUpdateEpochMillis = 0L
                RandomWeatherProvider()
            }
        } else {
            prefs.weatherConnectionStatus = WeatherConnectionStatus.DISABLED
            prefs.weatherLastUpdateEpochMillis = 0L
            RandomWeatherProvider()
        }
}
