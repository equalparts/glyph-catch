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
import dev.equalparts.glyph_catch.util.EventHelpers
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

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
            get() = EventHelpers.isHalloween()

        val christmas: Boolean
            get() = EventHelpers.isChristmas()

        val fullMoon: Boolean
            get() = EventHelpers.isFullMoon()
    }

    /**
     * Information about the phone itself, like battery level.
     */
    inner class PhoneState {
        var minutesOff: Int = 0
        var battery: Int = 100
        var isInteractive: Boolean = false

        val minutesOffOutsideBedtime: Int
            get() = sleep.minutesOffOutsideBedtime

        /**
         * Call this periodically to refresh the data.
         */
        fun refresh() {
            val batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as? BatteryManager
            val powerManager = applicationContext.getSystemService(POWER_SERVICE) as PowerManager

            battery = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

            val interactive = powerManager.isInteractive
            isInteractive = interactive

            if (interactive) {
                minutesOff = 0
            } else {
                minutesOff++
            }

            val now = ZonedDateTime.now()
            sleep.refresh(now, interactive, minutesOff)
        }
    }

    /**
     * Information about the sleep schedule mechanic.
     */
    inner class SleepState {
        private val prefs = PreferencesManager(applicationContext)
        private val sleepDurationMinutes = 8 * 60
        private val sleepGoalMinutes = 450

        private var cachedBedtimeMinutes = prefs.bedtimeMinutes
        private var previousBedtimeStart: ZonedDateTime? = null
        private var sleepGoalMetThisBedtime = false
        private var minutesOffAtSleepStart = 0
        private var baselineMinutesOff = 0
        private var wasBedtime = false

        var isBedtime: Boolean = false
            private set

        var hasSleepBonus: Boolean = false
            private set

        var minutesOffOutsideBedtime: Int = 0
            private set

        val bedtime: LocalTime
            get() = LocalTime.of(cachedBedtimeMinutes / 60, cachedBedtimeMinutes % 60)

        /**
         * Call this periodically to refresh the data.
         */
        fun refresh(now: ZonedDateTime, isInteractive: Boolean, currentMinutesOff: Int) {
            val bedtimeMinutes = prefs.bedtimeMinutes
            if (bedtimeMinutes != cachedBedtimeMinutes) {
                cachedBedtimeMinutes = bedtimeMinutes
            }

            val latestBedtimeStart = computeLatestBedtimeStart(now, cachedBedtimeMinutes)
            if (previousBedtimeStart != latestBedtimeStart) { // next sleep cycle
                sleepGoalMetThisBedtime = false
                hasSleepBonus = false
                minutesOffAtSleepStart = currentMinutesOff
            }
            previousBedtimeStart = latestBedtimeStart

            val isInBedtime = isInBedtime(now, latestBedtimeStart)
            if (!wasBedtime && isInBedtime) { // sleep cycle started
                minutesOffAtSleepStart = currentMinutesOff
            }

            if (isInBedtime) {
                trackSleepGoal(isInteractive, currentMinutesOff)
                baselineMinutesOff = currentMinutesOff
                minutesOffOutsideBedtime = 0
            } else {
                if (wasBedtime) {
                    baselineMinutesOff = currentMinutesOff
                    minutesOffOutsideBedtime = 0
                    if (sleepGoalMetThisBedtime) {
                        hasSleepBonus = true
                    }
                }
                minutesOffOutsideBedtime = (currentMinutesOff - baselineMinutesOff).coerceAtLeast(0)
            }

            if (isInteractive) {
                baselineMinutesOff = 0
                minutesOffOutsideBedtime = 0
            }

            isBedtime = isInBedtime
            wasBedtime = isInBedtime
        }

        private fun computeLatestBedtimeStart(now: ZonedDateTime, bedtimeMinutes: Int): ZonedDateTime {
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

        private fun isInBedtime(now: ZonedDateTime, windowStart: ZonedDateTime): Boolean {
            val windowEnd = windowStart.plusMinutes(sleepDurationMinutes.toLong())
            return !now.isBefore(windowStart) && now.isBefore(windowEnd)
        }

        private fun trackSleepGoal(isInteractive: Boolean, currentMinutesOff: Int) {
            if (!isInteractive) {
                val minutesSlept = currentMinutesOff - minutesOffAtSleepStart
                if (minutesSlept >= sleepGoalMinutes) {
                    sleepGoalMetThisBedtime = true
                }
            }
        }
    }

    /**
     * Information about the player.
     */
    inner class TrainerState {
        private val preferences = PreferencesManager(applicationContext)
        private val dayMillis = 24L * 60 * 60 * 1000

        var pokedexCount: Int = 0
        var currentPartnerDays: Int = 0

        /**
         * Yields `true` if the given Pokémon species is not in the current spawn queue
         * and has not been caught before.
         */
        fun hasNotFound(species: PokemonSpecies): Boolean = runBlocking {
            val db = PokemonDatabase.getInstance(applicationContext)
            if (db.pokemonDao().hasPokedexEntry(species.id)) {
                return@runBlocking false
            }
            return@runBlocking spawnQueue.none { it.pokemon.id == species.id }
        }

        /**
         * Yields `true` if the given Pokémon species are not in the current spawn queue
         * and have not been caught before.
         */
        fun hasNotFoundAny(species: List<PokemonSpecies>): Boolean = species.all { hasNotFound(it) }

        /**
         * Returns the days elapsed since any given Pokémon species was caught, or `null`
         * if none of the specified species have been caught.
         */
        fun daysSinceLastCaught(vararg speciesIds: Int): Int? = runBlocking {
            if (speciesIds.isEmpty()) return@runBlocking null
            val db = PokemonDatabase.getInstance(applicationContext)
            val lastCaught = db.pokemonDao().getLastCaughtAtForSpecies(speciesIds.toList()) ?: return@runBlocking null
            val now = System.currentTimeMillis()
            ((now - lastCaught) / dayMillis).coerceAtLeast(0).toInt()
        }

        /**
         * Returns the count of Pokémon at or above the specified level.
         */
        fun countPokemonAtLevel(minLevel: Int): Int = runBlocking {
            val db = PokemonDatabase.getInstance(applicationContext)
            db.pokemonDao().countPokemonAtLevel(minLevel)
        }

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
            val database = PokemonDatabase.getInstance(applicationContext)
            val dao = database.pokemonDao()
            val now = System.currentTimeMillis()

            runBlocking {
                pokedexCount = dao.getUniqueSpeciesCount()

                val activePartner = dao.getActiveTrainingPartner()
                if (activePartner == null) {
                    currentPartnerDays = 0
                    preferences.clearTrainingPartner()
                } else {
                    if (preferences.activeTrainingPartnerId != activePartner.id) {
                        preferences.markTrainingPartner(activePartner.id, now)
                    } else {
                        val startedAt = preferences.trainingPartnerBeganAt
                        currentPartnerDays = ((now - startedAt) / dayMillis).coerceAtLeast(0).toInt()
                    }
                }
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
