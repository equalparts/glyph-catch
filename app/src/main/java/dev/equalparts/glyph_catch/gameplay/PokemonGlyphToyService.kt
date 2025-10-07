package dev.equalparts.glyph_catch.gameplay

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.edit
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.animation.AnimationCoordinator
import dev.equalparts.glyph_catch.gameplay.animation.GlyphMatrixHelper
import dev.equalparts.glyph_catch.gameplay.spawner.GameplayContext
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnCadenceController
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnHistoryTracker
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnResult
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnRulesEngine
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnSnapshot
import dev.equalparts.glyph_catch.gameplay.spawner.createSpawnRules
import dev.equalparts.glyph_catch.util.GlyphMatrixService
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Simplified spawn data for persistence.
 */
@Serializable
data class PersistentSpawn(
    val pokemonId: Int,
    val poolName: String,
    val isSpecial: Boolean,
    val isConditional: Boolean,
    val screenOffDurationMinutes: Int,
    val spawnedAtMillis: Long = System.currentTimeMillis()
)

/**
 * The interactive Glyph Toy.
 */
class PokemonGlyphToyService : GlyphMatrixService("Pokemon-Glyph-Toy") {

    private val spawnQueue = mutableListOf<SpawnResult>()
    private lateinit var gameplayContext: GameplayContext
    private lateinit var spawnEngine: SpawnRulesEngine
    private var coroutineScope: CoroutineScope? = null
    private lateinit var db: PokemonDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var frameFactory: GlyphMatrixHelper
    private lateinit var animationCoordinator: AnimationCoordinator
    private var displayedSpawn: SpawnResult? = null
    private lateinit var cadenceController: SpawnCadenceController
    private lateinit var spawnHistory: SpawnHistoryTracker
    private var localClockJob: Job? = null
    private var aodActive = false

    /**
     * Called by the system when the service is first created.
     */
    override fun onCreate() {
        super.onCreate()
        db = PokemonDatabase.getInstance(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        frameFactory = GlyphMatrixHelper(applicationContext, GLYPH_MATRIX_SIZE)
        animationCoordinator = AnimationCoordinator(frameFactory) {
            glyphMatrixManager ?: error("GlyphMatrixManager not connected")
        }

        val weatherProvider = WeatherProviderFactory.create(applicationContext)
        gameplayContext = GameplayContext(applicationContext, weatherProvider, spawnQueue)

        val spawnRules = createSpawnRules(gameplayContext)
        spawnEngine = SpawnRulesEngine(spawnRules)
        spawnHistory = SpawnHistoryTracker(
            preferences = preferencesManager,
            pokemonDao = db.pokemonDao()
        )
        cadenceController = SpawnCadenceController(
            spawnEngine = spawnEngine,
            history = spawnHistory
        )
    }

    /**
     * Called by the system when the service is no longer used and is being removed.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (::animationCoordinator.isInitialized) {
            animationCoordinator.cancelActive()
        }
        localClockJob?.cancel()
        localClockJob = null
        coroutineScope?.cancel()
        coroutineScope = null
    }

    /**
     * Called by the system when the Glyph Toy is activated.
     */
    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        restoreSpawnQueue()
        spawnHistory.syncFromQueue(spawnQueue)
        aodActive = false
        tick()
        startLocalClock()
    }

    /**
     * Called by the system when the Glyph Toy is deactivated.
     */
    override fun performOnServiceDisconnected(context: Context) {
        super.performOnServiceDisconnected(context)
        localClockJob?.cancel()
        localClockJob = null
        saveSpawnQueue()
    }

    /**
     * Called by the system at every full minute mark: 10:30:00, 10:31:00, etc.
     */
    override fun onAodTick() {
        if (!aodActive) {
            aodActive = true
            localClockJob?.cancel()
            localClockJob = null
        }
        tick()
    }

    /**
     * Starts a local clock that ticks every minute until AOD becomes active.
     */
    private fun startLocalClock() {
        localClockJob?.cancel()
        localClockJob = coroutineScope?.launch(Dispatchers.Main) {
            while (!aodActive) {
                val calendar = Calendar.getInstance()
                val secondsUntilNextMinute = 60 - calendar.get(Calendar.SECOND)
                val millisUntilNextMinute = secondsUntilNextMinute * 1000L - calendar.get(Calendar.MILLISECOND)

                delay(millisUntilNextMinute)
                tick()
            }
        }
    }

    /**
     * Updates the game / display state. Might spawn a new Pokémon.
     */
    private fun tick() {
        gameplayContext.phone.refresh()
        gameplayContext.trainer.refresh()

        if (gameplayContext.sleep.hasSleepBonus) {
            val now = System.currentTimeMillis()
            if (preferencesManager.sleepBonusExpiresAt <= now) {
                preferencesManager.sleepBonusExpiresAt = now + SLEEP_BONUS_DURATION_MILLIS
            }
        } else if (preferencesManager.sleepBonusExpiresAt != 0L) {
            preferencesManager.sleepBonusExpiresAt = 0L
        }

        coroutineScope?.launch {
            db.activeItemDao().cleanupExpiredItems()
        }

        if (!preferencesManager.glyphToyHasTicked) {
            preferencesManager.glyphToyHasTicked = true
        }

        val now = System.currentTimeMillis()
        val snapshot = SpawnSnapshot(
            hasQueuedSpawns = spawnQueue.isNotEmpty(),
            isInteractive = gameplayContext.phone.isInteractive,
            screenOffMinutes = gameplayContext.phone.minutesOff,
            pokedexCount = gameplayContext.trainer.pokedexCount,
            isDuringSleepWindow = gameplayContext.sleep.isDuringSleepWindow
        )

        if (!snapshot.isInteractive) {
            cadenceController.maybeSpawn(now, snapshot)?.let { spawn ->
                addToQueue(spawn)
            }
        }

        updateGlyphMatrix()
    }

    /**
     * Adds a spawn to the queue, removing oldest non-special spawns if needed.
     */
    private fun addToQueue(spawn: SpawnResult) {
        synchronized(spawnQueue) {
            if (spawnQueue.size >= MAX_QUEUE_SIZE) {
                val indexToRemove = spawnQueue.indexOfFirst { !it.pool.isSpecial }
                if (indexToRemove != -1) {
                    val removed = spawnQueue.removeAt(indexToRemove)
                    Log.d(TAG, "Removed ${removed.pokemon.name} from queue (not special)")
                } else {
                    Log.d(TAG, "Queue full of special spawns, not adding ${spawn.pokemon.name}")
                    return
                }
            }

            spawnQueue.add(spawn)
            Log.d(TAG, "Spawned ${spawn.pokemon.name} from ${spawn.pool.name} pool")
            Log.d(TAG, "Queue size: ${spawnQueue.size}")

            sortQueueByRarity()
            spawnHistory.recordSpawn(spawn)
        }
        saveSpawnQueue()
    }

    /**
     * Sorts the spawn queue by rarity. Ensures the most interesting Pokémon is visible right away.
     */
    private fun sortQueueByRarity() {
        spawnQueue.sortWith(
            compareBy(
                { !it.pool.isSpecial }, // Special spawns first (false < true)
                { !it.pool.isConditional }, // Then conditional spawns
                { -it.pokemon.id } // Then by descending Pokédex number
            )
        )
    }

    /**
     * Shows the current Pokémon or digital clock on the Glyph Matrix.
     */
    private fun updateGlyphMatrix() {
        val currentSpawn = synchronized(spawnQueue) {
            spawnQueue.firstOrNull()
        }

        if (::animationCoordinator.isInitialized && animationCoordinator.isAnimating) {
            return
        }

        if (currentSpawn != null) {
            if (displayedSpawn !== currentSpawn) {
                val scope = coroutineScope
                if (scope != null) {
                    animationCoordinator.playSpawn(
                        scope = scope,
                        spawn = currentSpawn,
                        onDisplayed = { displayedSpawn = it }
                    )
                } else {
                    showPokemon(currentSpawn)
                }
            } else {
                showPokemon(currentSpawn)
            }
        } else {
            displayedSpawn = null
            showDigitalWatch()
        }
    }

    /**
     * Show the sprite of a spawned Pokémon on the Glyph Matrix.
     */
    private fun showPokemon(spawn: SpawnResult) {
        animationCoordinator.showPokemon(spawn.pokemon.id)
        displayedSpawn = spawn
    }

    /**
     * Show a digital clock on the Glyph Matrix.
     */
    private fun showDigitalWatch() {
        // TODO move to separate class
        val charHeight = 5
        val charSpacing = 1

        glyphMatrixManager?.let { gmm ->
            val currentTime = Calendar.getInstance()
            val timeText = if (DateFormat.is24HourFormat(applicationContext)) {
                val hour = currentTime.get(Calendar.HOUR_OF_DAY)
                val minute = currentTime.get(Calendar.MINUTE)
                String.format(Locale.US, "%02d:%02d", hour, minute)
            } else {
                val hour = currentTime.get(Calendar.HOUR)
                val minute = currentTime.get(Calendar.MINUTE)
                val displayHour = if (hour == 0) 12 else hour
                String.format(Locale.US, "%d:%02d", displayHour, minute)
            }

            fun getTextPixelWidth(text: String): Int {
                var width = 0
                for (i in text.indices) {
                    width += when (text[i]) {
                        ':' -> 1
                        '1' -> 3
                        else -> 4
                    }
                    if (i < text.length - 1) {
                        width += charSpacing
                    }
                }
                return width
            }

            val timeX = GLYPH_MATRIX_CENTER - getTextPixelWidth(timeText) / 2
            val timeY = GLYPH_MATRIX_CENTER - charHeight / 2

            val text = GlyphMatrixObject.Builder()
                .setText(timeText)
                .setPosition(timeX, timeY)
                .setBrightness(255)
                .build()

            val frame = GlyphMatrixFrame.Builder()
                .addTop(text)
                .build(applicationContext)

            gmm.setMatrixFrame(frame.render())
        }
    }

    /**
     * Called by the system when the user presses the touch button.
     */
    override fun onTouchPointLongPress() {
        Log.d(TAG, "Touch point long press detected")
        coroutineScope?.launch {
            val currentSpawn = synchronized(spawnQueue) {
                spawnQueue.firstOrNull()
            }
            if (currentSpawn != null) {
                catchPokemon(currentSpawn)
            }
        }
    }

    /**
     * Catches the currently spawned Pokémon.
     */
    private suspend fun catchPokemon(spawn: SpawnResult) {
        Log.d(TAG, "Catching ${spawn.pokemon.name}!")

        try {
            val caughtPokemon = CaughtPokemon(
                speciesId = spawn.pokemon.id,
                level = randomLevelForSpecies(spawn.pokemon.id),
                exp = 0,
                screenOffDurationMinutes = spawn.screenOffDurationMinutes,
                spawnPoolName = spawn.pool.name,
                isSpecialSpawn = spawn.pool.name.contains("special", ignoreCase = true),
                isConditionalSpawn = spawn.pool.name.contains("event", ignoreCase = true)
            )
            db.pokemonDao().insert(caughtPokemon)
            Log.d(TAG, "Successfully saved ${spawn.pokemon.name} to database")
            spawnHistory.recordCatch(spawn, caughtPokemon.caughtAt)

            synchronized(spawnQueue) {
                spawnQueue.remove(spawn)
            }
            saveSpawnQueue()

            animationCoordinator.cancelActive()
            displayedSpawn = null

            showCatchAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving caught Pokémon", e)
        }
    }

    /**
     * Plays a brief animation on the Glyph Matrix to confirm a successful catch.
     */
    private suspend fun showCatchAnimation() {
        animationCoordinator.playCatch()
        updateGlyphMatrix()
    }

    /**
     * Saves the current spawn queue to SharedPreferences.
     */
    private fun saveSpawnQueue() {
        synchronized(spawnQueue) {
            val persistentSpawns = spawnQueue.map { spawn ->
                PersistentSpawn(
                    pokemonId = spawn.pokemon.id,
                    poolName = spawn.pool.name,
                    isSpecial = spawn.pool.isSpecial,
                    isConditional = spawn.pool.isConditional,
                    screenOffDurationMinutes = spawn.screenOffDurationMinutes,
                    spawnedAtMillis = spawn.spawnedAtMillis
                )
            }

            val json = Json.encodeToString(persistentSpawns)
            applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit {
                    putString(KEY_SPAWN_QUEUE, json)
                }
            Log.d(TAG, "Saved ${persistentSpawns.size} spawns to storage")
        }
    }

    /**
     * Restores the spawn queue from SharedPreferences.
     */
    private fun restoreSpawnQueue() {
        synchronized(spawnQueue) {
            spawnQueue.clear()
            displayedSpawn = null

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val json = prefs.getString(KEY_SPAWN_QUEUE, null) ?: return

            try {
                val persistentSpawns: List<PersistentSpawn> = Json.decodeFromString(json)
                persistentSpawns.forEach { persistent ->
                    val pokemon = Pokemon.all[persistent.pokemonId]
                    if (pokemon != null) {
                        val pool = spawnEngine.rules.pools.find { it.name == persistent.poolName }!!
                        val spawn = SpawnResult(
                            pokemon = pokemon,
                            pool = pool,
                            screenOffDurationMinutes = persistent.screenOffDurationMinutes,
                            spawnedAtMillis = persistent.spawnedAtMillis
                        )
                        spawnQueue.add(spawn)
                    }
                }
                Log.d(TAG, "Restored ${spawnQueue.size} spawns from storage")
                sortQueueByRarity()
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring spawn queue", e)
                prefs.edit { remove(KEY_SPAWN_QUEUE) }
            }
        }
    }

    private fun randomLevelForSpecies(speciesId: Int): Int {
        val species = Pokemon[speciesId] ?: return Random.nextInt(18, 41)
        val nextLevelRequirement = species.evolvesTo
            .mapNotNull { Pokemon[it] }
            .mapNotNull { (it.evolutionRequirement as? EvolutionRequirement.Level)?.level }
            .minOrNull()

        if (nextLevelRequirement != null && nextLevelRequirement > 2) {
            val minExclusive = floor(nextLevelRequirement * 0.5).toInt()
            val minLevel = (minExclusive + 1).coerceAtLeast(1)
            val maxLevel = (nextLevelRequirement - 2).coerceAtLeast(minLevel)

            return if (maxLevel >= minLevel) {
                Random.nextInt(minLevel, maxLevel + 1)
            } else {
                minLevel
            }
        }

        return Random.nextInt(18, 41)
    }
    companion object {
        private const val TAG = "PokemonGlyphToy"
        private const val MAX_QUEUE_SIZE = 3
        private const val PREFS_NAME = "pokemon_glyph_toy_prefs"
        private const val KEY_SPAWN_QUEUE = "spawn_queue"
        private const val SLEEP_BONUS_DURATION_MILLIS = 24L * 60 * 60 * 1000

        private const val GLYPH_MATRIX_SIZE = 25 // 25x25 circular display
        private const val GLYPH_MATRIX_CENTER = GLYPH_MATRIX_SIZE / 2 // Center index (12, which is the 13th pixel)
    }
}







