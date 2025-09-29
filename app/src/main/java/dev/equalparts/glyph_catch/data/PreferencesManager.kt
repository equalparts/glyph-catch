package dev.equalparts.glyph_catch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Manages app preferences including weather provider settings.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var openWeatherMapApiKey: String?
        get() = prefs.getString(KEY_OPENWEATHER_API, null)
        set(value) = prefs.edit { putString(KEY_OPENWEATHER_API, value) }

    var useOpenWeatherMap: Boolean
        get() = prefs.getBoolean(KEY_USE_OPENWEATHER, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_OPENWEATHER, value) }

    var weatherLatitude: Float
        get() = prefs.getFloat(KEY_WEATHER_LAT, 0f)
        set(value) = prefs.edit { putFloat(KEY_WEATHER_LAT, value) }

    var weatherLongitude: Float
        get() = prefs.getFloat(KEY_WEATHER_LON, 0f)
        set(value) = prefs.edit { putFloat(KEY_WEATHER_LON, value) }

    var weatherLocationName: String?
        get() = prefs.getString(KEY_WEATHER_LOCATION_NAME, null)
        set(value) = prefs.edit { putString(KEY_WEATHER_LOCATION_NAME, value) }

    var weatherConnectionStatus: WeatherConnectionStatus
        get() = WeatherConnectionStatus.fromStored(prefs.getString(KEY_WEATHER_CONNECTION_STATUS, null))
        set(value) = prefs.edit { putString(KEY_WEATHER_CONNECTION_STATUS, value.name) }

    var weatherLastUpdateEpochMillis: Long
        get() = prefs.getLong(KEY_WEATHER_LAST_UPDATE_EPOCH, 0L)
        set(value) = prefs.edit { putLong(KEY_WEATHER_LAST_UPDATE_EPOCH, value) }

    var bedtimeMinutes: Int
        get() = prefs.getInt(KEY_BEDTIME_MINUTES, DEFAULT_BEDTIME_MINUTES)
        set(value) {
            val normalized = ((value % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
            prefs.edit { putInt(KEY_BEDTIME_MINUTES, normalized) }
        }

    var playerStartDate: Long
        get() = prefs.getLong(KEY_PLAYER_START_DATE, 0L)
        set(value) = prefs.edit { putLong(KEY_PLAYER_START_DATE, value) }

    var glyphToyHasTicked: Boolean
        get() = prefs.getBoolean(KEY_GLYPH_TOY_TICKED, false)
        set(value) = prefs.edit { putBoolean(KEY_GLYPH_TOY_TICKED, value) }

    fun watchGlyphToyHasTicked(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == KEY_GLYPH_TOY_TICKED) {
                trySend(glyphToyHasTicked)
            }
        }

        trySend(glyphToyHasTicked)
        registerListener(listener)

        awaitClose { unregisterListener(listener) }
    }

    fun hasValidWeatherConfig(): Boolean = !openWeatherMapApiKey.isNullOrBlank() &&
        weatherLatitude != 0f &&
        weatherLongitude != 0f

    fun getWeatherConfig(): WeatherConfig = WeatherConfig(
        useOpenWeather = useOpenWeatherMap,
        apiKey = openWeatherMapApiKey,
        latitude = weatherLatitude,
        longitude = weatherLongitude
    )

    fun watchWeatherConfig(): Flow<WeatherConfig> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == null || changedKey in WEATHER_CONFIG_KEYS) {
                trySend(getWeatherConfig())
            }
        }

        trySend(getWeatherConfig())
        registerListener(listener)

        awaitClose { unregisterListener(listener) }
    }.distinctUntilChanged()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "glyph_catch_prefs"
        private const val KEY_OPENWEATHER_API = "openweather_api_key"
        private const val KEY_USE_OPENWEATHER = "use_openweather"
        private const val KEY_WEATHER_LAT = "weather_latitude"
        private const val KEY_WEATHER_LON = "weather_longitude"
        private const val KEY_WEATHER_LOCATION_NAME = "weather_location_name"
        private const val KEY_WEATHER_CONNECTION_STATUS = "weather_connection_status"
        private const val KEY_WEATHER_LAST_UPDATE_EPOCH = "weather_last_update_epoch"
        private const val KEY_BEDTIME_MINUTES = "bedtime_minutes"
        private const val KEY_PLAYER_START_DATE = "player_start_date"
        private const val KEY_GLYPH_TOY_TICKED = "glyph_toy_has_ticked"
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_BEDTIME_MINUTES = 23 * 60

        private val WEATHER_CONFIG_KEYS = setOf(
            KEY_USE_OPENWEATHER,
            KEY_OPENWEATHER_API,
            KEY_WEATHER_LAT,
            KEY_WEATHER_LON
        )
    }
}
