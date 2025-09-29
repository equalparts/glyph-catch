package dev.equalparts.glyph_catch.gameplay

import dev.equalparts.glyph_catch.gameplay.spawner.Weather
import dev.equalparts.glyph_catch.gameplay.spawner.WeatherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

/**
 * Default weather provider that generates a random weather condition per day.
 */
class RandomWeatherProvider : WeatherProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val weatherState = MutableStateFlow(calculateWeatherForToday())

    init {
        scope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                val updated = calculateWeatherForToday()
                if (updated != weatherState.value) {
                    weatherState.value = updated
                }
            }
        }
    }

    override fun getCurrentWeather(): Weather {
        val latest = calculateWeatherForToday()
        if (latest != weatherState.value) {
            weatherState.value = latest
        }
        return weatherState.value
    }

    override fun watchWeather(): Flow<Weather> = weatherState

    private fun calculateWeatherForToday(): Weather {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return generateDailyWeather(year, dayOfYear)
    }

    private fun generateDailyWeather(year: Int, dayOfYear: Int): Weather {
        val seed = (year * 1000L) + dayOfYear
        val random = Random(seed)
        val roll = random.nextFloat()

        return when {
            roll < 0.75f -> Weather.CLEAR // 70% chance
            roll < 0.90f -> Weather.RAIN // 15% chance
            roll < 0.95f -> Weather.THUNDERSTORM // 5% chance
            else -> Weather.SNOW // 5% chance
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 60 * 60 * 1000L
    }
}
