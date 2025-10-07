package dev.equalparts.glyph_catch.gameplay

import android.util.Log
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.WeatherConnectionStatus
import dev.equalparts.glyph_catch.gameplay.spawner.Weather
import dev.equalparts.glyph_catch.gameplay.spawner.WeatherProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Weather provider that fetches live weather data from OpenWeatherMap API.
 */
class OpenWeatherMapProvider(
    private val prefs: PreferencesManager,
    private val apiKey: String,
    private val latitude: Double,
    private val longitude: Double
) : WeatherProvider {

    private var cachedWeather = Weather.CLEAR
    private var lastFetchTime = 0L
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val weatherState = MutableStateFlow(cachedWeather)

    init {
        if (prefs.weatherLastUpdateEpochMillis == 0L) {
            prefs.weatherConnectionStatus = WeatherConnectionStatus.NEVER_CONNECTED
        }
        fetchWeatherAsync()
    }

    override fun getCurrentWeather(): Weather {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime > TTL) {
            fetchWeatherAsync()
        }
        return cachedWeather
    }

    override fun watchWeather(): Flow<Weather> = weatherState

    private fun fetchWeatherAsync() {
        coroutineScope.launch {
            val fetchStartedAt = System.currentTimeMillis()
            try {
                val weather = fetchWeatherFromApi()
                cachedWeather = weather
                weatherState.value = weather
                lastFetchTime = fetchStartedAt
                prefs.weatherConnectionStatus = WeatherConnectionStatus.CONNECTED
                prefs.weatherLastUpdateEpochMillis = fetchStartedAt
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch weather", e)
                if (prefs.weatherLastUpdateEpochMillis == 0L) {
                    prefs.weatherConnectionStatus = WeatherConnectionStatus.FAILED
                }
            }
        }
    }

    private suspend fun fetchWeatherFromApi(): Weather = withContext(Dispatchers.IO) {
        val encodedApiKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = URL(
            "https://api.openweathermap.org/data/2.5/weather?" +
                "lat=$latitude&lon=$longitude&appid=$encodedApiKey"
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected response code ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val weatherArray = json.optJSONArray("weather")
                ?: throw IOException("Missing weather array in response")

            if (weatherArray.length() == 0) {
                throw IOException("Empty weather array in response")
            }

            val weatherObj = weatherArray.getJSONObject(0)
            val weatherId = weatherObj.getInt("id")

            return@withContext mapWeatherCode(weatherId)
        } finally {
            connection.disconnect()
        }
    }

    private fun mapWeatherCode(code: Int): Weather = when (code) {
        in 200..299 -> Weather.THUNDERSTORM // Thunderstorm group
        in 300..599 -> Weather.RAIN // Drizzle, Rain group
        in 600..699 -> Weather.SNOW // Snow group
        else -> Weather.CLEAR // Clear, Clouds, Atmosphere, etc.
    }

    companion object {
        private const val TAG = "OpenWeatherMapProvider"
        private const val TTL = 60 * 60 * 1000 // 1 hour
    }
}
