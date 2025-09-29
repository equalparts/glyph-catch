package dev.equalparts.glyph_catch.gameplay.spawner

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Context.POWER_SERVICE
import android.os.BatteryManager
import android.os.PowerManager
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.math.abs

/**
 * Provides external data for the spawn rules, such as current time and weather.
 */
@Suppress("unused")
data class GameplayContext(
    val applicationContext: Context,
    val weatherProvider: WeatherProvider,
    var spawnQueue: List<SpawnResult>
) {
    val time = TimeConditions()
    val weather = WeatherConditions()
    val season = SeasonalConditions()
    val events = EventConditions()
    val sleep = SleepState()
    val phone = PhoneState()
    val trainer = TrainerState()

    /**
     * Conditions related to time of day.
     */
    inner class TimeConditions {
        val night: Boolean
            get() {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                return hour < 6 || hour >= 20
            }

        val day: Boolean
            get() {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                return hour in 6..17
            }
    }

    /**
     * Conditions related to current weather.
     */
    inner class WeatherConditions {
        val rain: Boolean
            get() = weatherProvider.getCurrentWeather() == Weather.RAIN

        val thunderstorm: Boolean
            get() = weatherProvider.getCurrentWeather() == Weather.THUNDERSTORM

        val snow: Boolean
            get() = weatherProvider.getCurrentWeather() == Weather.SNOW

        val clear: Boolean
            get() = weatherProvider.getCurrentWeather() == Weather.CLEAR
    }

    /**
     * Conditions related to current season.
     */
    inner class SeasonalConditions {
        val winter: Boolean
            get() {
                val month = Calendar.getInstance().get(Calendar.MONTH)
                return month in listOf(Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY)
            }

        val spring: Boolean
            get() {
                val month = Calendar.getInstance().get(Calendar.MONTH)
                return month in listOf(Calendar.MARCH, Calendar.APRIL, Calendar.MAY)
            }

        val summer: Boolean
            get() {
                val month = Calendar.getInstance().get(Calendar.MONTH)
                return month in listOf(Calendar.JUNE, Calendar.JULY, Calendar.AUGUST)
            }

        val autumn: Boolean
            get() {
                val month = Calendar.getInstance().get(Calendar.MONTH)
                return month in listOf(Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER)
            }
    }

    /**
     * Conditions related to special events.
     */
    inner class EventConditions {
        val halloween: Boolean
            get() {
                val cal = Calendar.getInstance()
                val month = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)
                return month == Calendar.OCTOBER && day in 25..31
            }

        val christmas: Boolean
            get() {
                val cal = Calendar.getInstance()
                val month = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)
                return month == Calendar.DECEMBER && day in 19..26
            }

        val fullMoon: Boolean
            get() {
                val synodicMonthDays = 29.530588853
                val millisPerDay = 86_400_000.0
                val fullMoonThresholdDays = 0.9
                val referenceNewMoon = ZonedDateTime.of(2000, 1, 6, 18, 14, 0, 0, ZoneOffset.UTC)

                val nowUtc = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
                val daysSinceReference = Duration.between(referenceNewMoon, nowUtc).toMillis() / millisPerDay
                val moonAge = ((daysSinceReference % synodicMonthDays) + synodicMonthDays) % synodicMonthDays
                val delta = abs(moonAge - synodicMonthDays / 2.0)
                return delta <= fullMoonThresholdDays
            }
    }

    /**
     * Information about the phone itself, like battery level.
     */
    inner class PhoneState {
        var minutesOff: Int = 0
        var battery: Int = 100

        val minutesOffForSpawns: Int
            get() = sleep.minutesOutsideSleep

        /**
         * Call this periodically to refresh the data.
         */
        fun refresh() {
            val batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as? BatteryManager
            val powerManager = applicationContext.getSystemService(POWER_SERVICE) as PowerManager

            battery = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

            val isInteractive = powerManager.isInteractive

            if (isInteractive) {
                minutesOff = 0
            } else {
                minutesOff++
            }

            val now = ZonedDateTime.now()
            sleep.refresh(now, isInteractive, minutesOff)
        }
    }

    inner class SleepState {
        private val prefs = PreferencesManager(applicationContext)
        private val sleepDurationMinutes = 8 * 60
        private val sleepGoalMinutes = 450

        private var cachedBedtimeMinutes = prefs.bedtimeMinutes
        private var lastWindowStart: ZonedDateTime? = null
        private var goalReachedThisWindow = false
        private var bonusActive = false
        private var minutesOffAtWindowStart = 0
        private var minutesOffBaseline = 0
        private var outsideSleepMinutes = 0
        private var wasDuringSleepWindow = false

        var isDuringSleepWindow: Boolean = false
            private set

        val hasSleepBonus: Boolean
            get() = bonusActive

        val minutesOutsideSleep: Int
            get() = outsideSleepMinutes

        val bedtime: LocalTime
            get() = LocalTime.of(cachedBedtimeMinutes / 60, cachedBedtimeMinutes % 60)

        fun refresh(now: ZonedDateTime, isInteractive: Boolean, currentMinutesOff: Int) {
            val bedtimeMinutes = prefs.bedtimeMinutes
            if (bedtimeMinutes != cachedBedtimeMinutes) {
                cachedBedtimeMinutes = bedtimeMinutes
            }

            val windowStart = computeWindowStart(now, cachedBedtimeMinutes)
            val windowChanged = lastWindowStart != windowStart
            if (windowChanged) {
                goalReachedThisWindow = false
                bonusActive = false
                minutesOffAtWindowStart = currentMinutesOff
            }
            lastWindowStart = windowStart

            val windowEnd = windowStart.plusMinutes(sleepDurationMinutes.toLong())
            val currentlyInWindow = !now.isBefore(windowStart) && now.isBefore(windowEnd)

            if (currentlyInWindow) {
                if (!wasDuringSleepWindow) {
                    minutesOffAtWindowStart = currentMinutesOff
                }
                if (!isInteractive) {
                    val minutesInWindow = currentMinutesOff - minutesOffAtWindowStart
                    if (minutesInWindow >= sleepGoalMinutes) {
                        goalReachedThisWindow = true
                    }
                }
                minutesOffBaseline = currentMinutesOff
                outsideSleepMinutes = 0
            } else {
                if (wasDuringSleepWindow) {
                    minutesOffBaseline = currentMinutesOff
                    outsideSleepMinutes = 0
                }
                outsideSleepMinutes = (currentMinutesOff - minutesOffBaseline).coerceAtLeast(0)
                if (goalReachedThisWindow) {
                    bonusActive = true
                }
            }

            if (isInteractive) {
                minutesOffBaseline = currentMinutesOff
                outsideSleepMinutes = 0
            }

            isDuringSleepWindow = currentlyInWindow
            wasDuringSleepWindow = currentlyInWindow
        }

        private fun computeWindowStart(now: ZonedDateTime, bedtimeMinutes: Int): ZonedDateTime {
            val bedtimeClock = LocalTime.of(bedtimeMinutes / 60, bedtimeMinutes % 60)
            val candidate = now
                .withHour(bedtimeClock.hour)
                .withMinute(bedtimeClock.minute)
                .withSecond(0)
                .withNano(0)

            return if (candidate.isAfter(now)) {
                candidate.minusDays(1)
            } else {
                candidate
            }
        }
    }

    /**
     * Information about the player.
     */
    inner class TrainerState {
        var pokedexCount: Int = 0
        var currentPartnerDays: Int = 0
        var daysSinceLastFighterCaught: Int = 999 // Start high so first catch works

        /**
         * Yields `true` if the given Pokémon species is not in the current spawn queue
         * and has not been caught before.
         */
        fun hasNotFound(species: PokemonSpecies): Boolean = runBlocking {
            val db = PokemonDatabase.getInstance(applicationContext)
            if (db.pokemonDao().countBySpeciesId(species.id) > 0) return@runBlocking false
            return@runBlocking spawnQueue.none { it.pokemon.id == species.id }
        }

        /**
         * Yields `true` if the given Pokémon species are not in the current spawn queue
         * and have not been caught before.
         */
        fun hasNotFoundAny(species: List<PokemonSpecies>): Boolean = species.all { hasNotFound(it) }

        /**
         * Yields `true` if the player possesses one or more of the given item.
         */
        fun hasItem(item: Item): Boolean = runBlocking {
            val db = PokemonDatabase.getInstance(applicationContext)
            val inventoryItem = db.inventoryDao().getItem(item.ordinal)
            (inventoryItem?.quantity ?: 0) > 0
        }

        /**
         * Yields `true` if the effect of the given item is currently active.
         */
        fun isUsingItem(item: Item): Boolean = runBlocking {
            val db = PokemonDatabase.getInstance(applicationContext)
            val activeItem = db.activeItemDao().getActiveItem(item.ordinal)
            activeItem != null
        }

        /**
         * Call this periodically to refresh the data.
         */
        fun refresh() {
            pokedexCount = runBlocking {
                val db = PokemonDatabase.getInstance(applicationContext)
                db.pokemonDao().getUniqueSpeciesCount()
            }
        }
    }
}

/**
 * A service that fetches current weather information.
 */
interface WeatherProvider {
    fun getCurrentWeather(): Weather
    fun watchWeather(): Flow<Weather>
}

/**
 * Supported weather conditions.
 */
enum class Weather {
    CLEAR, RAIN, THUNDERSTORM, SNOW
}
