package dev.equalparts.glyph_catch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    var hasDiscoveredSuperRod: Boolean
        get() = prefs.getBoolean(KEY_SUPER_ROD_DISCOVERED, false)
        set(value) = prefs.edit { putBoolean(KEY_SUPER_ROD_DISCOVERED, value) }

    var isSuperRodIndicatorDismissed: Boolean
        get() = prefs.getBoolean(KEY_SUPER_ROD_INDICATOR_DISMISSED, false)
        set(value) = prefs.edit { putBoolean(KEY_SUPER_ROD_INDICATOR_DISMISSED, value) }

    var sleepBonusExpiresAt: Long
        get() = prefs.getLong(KEY_SLEEP_BONUS_EXPIRES_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_SLEEP_BONUS_EXPIRES_AT, value) }

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

    fun shouldShowSuperRodIndicator(): Boolean = hasDiscoveredSuperRod && !isSuperRodIndicatorDismissed

    fun watchSuperRodIndicator(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == null || changedKey in SUPER_ROD_KEYS) {
                trySend(shouldShowSuperRodIndicator())
            }
        }

        trySend(shouldShowSuperRodIndicator())
        registerListener(listener)

        awaitClose { unregisterListener(listener) }
    }.distinctUntilChanged()

    fun markSuperRodDiscovered() {
        if (!hasDiscoveredSuperRod) {
            hasDiscoveredSuperRod = true
            isSuperRodIndicatorDismissed = false
        }
    }

    fun markSuperRodIndicatorSeen() {
        if (!isSuperRodIndicatorDismissed) {
            isSuperRodIndicatorDismissed = true
        }
    }

    fun isSleepBonusActive(now: Long = System.currentTimeMillis()): Boolean = sleepBonusExpiresAt > now

    fun watchSleepBonusStatus(): Flow<Boolean> = callbackFlow {
        fun emitStatus() {
            trySend(isSleepBonusActive())
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == null || changedKey == KEY_SLEEP_BONUS_EXPIRES_AT) {
                emitStatus()
            }
        }

        emitStatus()
        registerListener(listener)

        val tickerJob = launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val expiresAt = sleepBonusExpiresAt
                val delayMillis = if (expiresAt <= now) {
                    SLEEP_BONUS_POLL_INACTIVE_MILLIS
                } else {
                    (expiresAt - now).coerceIn(1_000L, SLEEP_BONUS_POLL_ACTIVE_MILLIS)
                }
                delay(delayMillis)
                emitStatus()
            }
        }

        awaitClose {
            unregisterListener(listener)
            tickerJob.cancel()
        }
    }.distinctUntilChanged()

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
        private const val KEY_SUPER_ROD_DISCOVERED = "super_rod_discovered"
        private const val KEY_SUPER_ROD_INDICATOR_DISMISSED = "super_rod_indicator_dismissed"
        private const val KEY_SLEEP_BONUS_EXPIRES_AT = "sleep_bonus_expires_at"
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_BEDTIME_MINUTES = 23 * 60
        private const val SLEEP_BONUS_POLL_ACTIVE_MILLIS = 30_000L
        private const val SLEEP_BONUS_POLL_INACTIVE_MILLIS = 60_000L

        private val WEATHER_CONFIG_KEYS = setOf(
            KEY_USE_OPENWEATHER,
            KEY_OPENWEATHER_API,
            KEY_WEATHER_LAT,
            KEY_WEATHER_LON
        )

        private val SUPER_ROD_KEYS = setOf(
            KEY_SUPER_ROD_DISCOVERED,
            KEY_SUPER_ROD_INDICATOR_DISMISSED
        )
    }
}
