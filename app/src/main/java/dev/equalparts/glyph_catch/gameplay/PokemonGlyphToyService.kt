package dev.equalparts.glyph_catch.gameplay

import android.content.Context
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.edit
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.InventoryItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.debug.DebugCaptureManager
import dev.equalparts.glyph_catch.debug.DebugExceptionTracker
import dev.equalparts.glyph_catch.debug.DebugSnapshot
import dev.equalparts.glyph_catch.gameplay.animation.AnimationCoordinator
import dev.equalparts.glyph_catch.gameplay.animation.GlyphMatrixHelper
import dev.equalparts.glyph_catch.gameplay.spawner.GameplayContext
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnCadenceController
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnContext
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnHistoryTracker
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnResult
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnRulesEngine
import dev.equalparts.glyph_catch.gameplay.spawner.createSpawnRules
import dev.equalparts.glyph_catch.util.GlyphMatrixService
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.random.Random
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

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

    private var coroutineScope: CoroutineScope? = null

    private lateinit var db: PokemonDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var frameFactory: GlyphMatrixHelper
    private lateinit var animationCoordinator: AnimationCoordinator

    private lateinit var gameplayContext: GameplayContext
    private lateinit var spawnEngine: SpawnRulesEngine
    private lateinit var spawnHistory: SpawnHistoryTracker
    private lateinit var cadenceController: SpawnCadenceController

    private var localClockJob: Job? = null
    private var aodActive = false

    private val animationWakeLockMutex = Any()
    private var animationWakeLock: PowerManager.WakeLock? = null
    private var activeAnimationWakeLockHolders = 0

    private val spawnQueue = mutableListOf<SpawnResult>()
    private var displayedSpawn: SpawnResult? = null

    private lateinit var debugCapture: DebugCaptureManager
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleCoroutineException(throwable)
    }

    /**
     * Called by the system when the service is first created.
     */
    override fun onCreate() {
        super.onCreate()
        DebugExceptionTracker.install(applicationContext)

        coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)

        db = PokemonDatabase.getInstance(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        frameFactory = GlyphMatrixHelper(applicationContext, GLYPH_MATRIX_SIZE)
        animationCoordinator = AnimationCoordinator(
            glyphFrameHelper = frameFactory,
            glyphMatrixManagerProvider = { glyphMatrixManager ?: error("GlyphMatrixManager not connected") },
            animationDispatcher = Dispatchers.Main.immediate,
            onAnimationStart = { acquireAnimationWakeLock() },
            onAnimationEnd = { releaseAnimationWakeLock() }
        )

        val weatherProvider = WeatherProviderFactory.create(applicationContext)
        gameplayContext = GameplayContext(applicationContext, weatherProvider, spawnQueue)
        spawnEngine = SpawnRulesEngine(createSpawnRules(gameplayContext))
        spawnHistory = SpawnHistoryTracker(preferences = preferencesManager, pokemonDao = db.pokemonDao())
        cadenceController = SpawnCadenceController(spawnEngine = spawnEngine, history = spawnHistory)

        val powerManager = applicationContext.getSystemService(PowerManager::class.java)
        animationWakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )

        debugCapture = DebugCaptureManager.shared(applicationContext)
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
        synchronized(animationWakeLockMutex) {
            animationWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    runCatching { wakeLock.release() }
                        .onFailure { error -> Log.w(TAG, "Unable to release animation wake lock on destroy", error) }
                }
            }
            animationWakeLock = null
            activeAnimationWakeLockHolders = 0
        }
    }

    /**
     * Called by the system when the Glyph Toy is activated.
     */
    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        restoreSpawnQueue()
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

        if (gameplayContext.phone.isInteractive) {
            spawnHistory.resetAfterScreenOn()
        }

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
        val spawnContext = SpawnContext(
            hasQueuedSpawns = spawnQueue.isNotEmpty(),
            phoneIsInteractive = gameplayContext.phone.isInteractive,
            phoneMinutesOff = gameplayContext.phone.minutesOff,
            trainerPokedexCount = gameplayContext.trainer.pokedexCount,
            isBedtime = gameplayContext.sleep.isBedtime
        )

        if (!spawnContext.phoneIsInteractive) {
            val minutesOffSnapshot = gameplayContext.phone.minutesOff
            val isBedtimeSnapshot = gameplayContext.sleep.isBedtime
            coroutineScope?.launch {
                applyTrainingExp(minutesOffSnapshot, isBedtimeSnapshot)
            }
        }

        val decision = cadenceController.maybeSpawn(now, spawnContext)
        var spawned = decision.spawn
        if (spawned != null && preferencesManager.isRepelActive) {
            val alreadyCaught = runBlocking { db.pokemonDao().hasPokedexEntry(spawned.pokemon.id) }
            if (alreadyCaught) {
                spawned = null
            }
        }

        if (spawned != null) {
            addToQueue(spawned)
        }

        coroutineScope?.launch {
            debugCapture.log("tick", currentDebugSnapshot()) {
                buildJsonObject {
                    put(
                        "decision",
                        buildJsonObject {
                            put("reason", JsonPrimitive(decision.debug.reason.name.lowercase(Locale.US)))
                            put("minutesAccumulated", JsonPrimitive(decision.debug.minutesAccumulated))
                            put("baseChance", JsonPrimitive(decision.debug.baseChance))
                            decision.debug.roll?.let { put("roll", JsonPrimitive(it)) }
                            decision.debug.initialPool?.let { put("initialPool", JsonPrimitive(it)) }
                            decision.debug.finalPool?.let { put("finalPool", JsonPrimitive(it)) }
                            decision.debug.rerollTargetPool?.let { put("rerollTargetPool", JsonPrimitive(it)) }
                            put("rerollsRequested", JsonPrimitive(decision.debug.rerollsRequested))
                            put("rerollsUsed", JsonPrimitive(decision.debug.rerollsUsed))
                        }
                    )
                    spawned?.let { spawn ->
                        put(
                            "spawn",
                            buildJsonObject {
                                put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                                put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                                put("pool", JsonPrimitive(spawn.pool.name))
                                put("screenOffMinutes", JsonPrimitive(spawn.screenOffDurationMinutes))
                            }
                        )
                    }
                }
            }
        }

        updateGlyphMatrix()
    }

    /**
     * Runs during ticks to level and/or evolve Pokémon.
     */
    private suspend fun applyTrainingExp(minutesOff: Int, isBedtime: Boolean) {
        val dao = db.pokemonDao()
        val active = dao.getActiveTrainingPartner() ?: return
        val intervalBonus = if (!isBedtime && minutesOff > 0 && minutesOff % TRAINING_EXP_BONUS_INTERVAL_MINUTES == 0) {
            TRAINING_EXP_BONUS_AMOUNT
        } else {
            0
        }
        val gainedExp = TRAINING_EXP_PER_MINUTE + intervalBonus
        val result = TrainingProgression.expResult(active.level, active.exp, gainedExp) ?: return
        dao.updateTrainingProgress(active.id, result.exp, result.level)
        if (result.leveledUp) {
            maybeTriggerEvolution(active.copy(level = result.level, exp = result.exp))
        }
    }

    /**
     * Called when a Pokémon levels up to trigger an evolution when needed.
     */
    private suspend fun maybeTriggerEvolution(pokemon: CaughtPokemon) {
        val species = Pokemon[pokemon.speciesId] ?: return
        val target = species.evolvesTo
            .mapNotNull { Pokemon[it] }
            .firstOrNull { candidate ->
                val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Level
                requirement != null && pokemon.level >= requirement.level
            } ?: return
        db.pokemonDao().evolvePokemon(
            pokemonId = pokemon.id,
            newSpeciesId = target.id,
            newLevel = pokemon.level,
            newExp = pokemon.exp
        )
        db.pokemonDao().recordPokedexEntry(target.id)
        preferencesManager.enqueueEvolutionNotification(
            previousSpeciesId = species.id,
            newSpeciesId = target.id
        )
    }

    /**
     * Adds a spawn to the queue, removing oldest non-special spawns if needed.
     */
    private fun addToQueue(spawn: SpawnResult) {
        var removed: SpawnResult? = null

        synchronized(spawnQueue) {
            if (spawnQueue.size >= MAX_QUEUE_SIZE) {
                val indexToRemove = spawnQueue.indexOfFirst { !it.pool.isSpecial }
                if (indexToRemove != -1) {
                    removed = spawnQueue.removeAt(indexToRemove)
                    Log.d(TAG, "Removed ${removed.pokemon.name} from queue (not special)")
                } else {
                    Log.d(TAG, "Queue full of special spawns, not adding ${spawn.pokemon.name}")
                    return
                }
            }

            spawnQueue.add(spawn)
            Log.d(TAG, "Spawned ${spawn.pokemon.name} from ${spawn.pool.name} pool")
            sortQueueByRarity()
            spawnHistory.updateActiveQueue(spawnQueue, newSpawn = spawn)
        }

        saveSpawnQueue()

        val snapshot = currentDebugSnapshot()

        coroutineScope?.launch {
            removed?.let { removedSpawn ->
                debugCapture.log("queue_evicted", snapshot) {
                    buildJsonObject {
                        put("reason", JsonPrimitive("fifo_overflow"))
                        put("removedPokemonId", JsonPrimitive(removedSpawn.pokemon.id))
                        put("removedPokemonName", JsonPrimitive(removedSpawn.pokemon.name))
                        put("removedPool", JsonPrimitive(removedSpawn.pool.name))
                    }
                }
            }

            debugCapture.log("spawn_enqueued", snapshot) {
                buildJsonObject {
                    put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                    put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                    put("pool", JsonPrimitive(spawn.pool.name))
                }
            }
        }
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
            debugCapture.log("input_long_press", currentDebugSnapshot()) {
                buildJsonObject {
                    put("hadSpawn", JsonPrimitive(currentSpawn != null))
                    currentSpawn?.let { spawn ->
                        put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                        put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                        put("pool", JsonPrimitive(spawn.pool.name))
                    }
                }
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
            val alreadyDiscovered = db.pokemonDao().hasPokedexEntry(spawn.pokemon.id)
            val caughtPokemon = CaughtPokemon(
                speciesId = spawn.pokemon.id,
                spawnedAt = spawn.spawnedAtMillis,
                level = randomLevelForSpecies(spawn.pokemon.id),
                exp = 0,
                screenOffDurationMinutes = spawn.screenOffDurationMinutes,
                spawnPoolName = spawn.pool.name,
                isSpecialSpawn = spawn.pool.name.contains("special", ignoreCase = true),
                isConditionalSpawn = spawn.pool.name.contains("event", ignoreCase = true)
            )
            db.pokemonDao().insert(caughtPokemon)
            db.pokemonDao().recordPokedexEntry(spawn.pokemon.id)
            Log.d(TAG, "Successfully saved ${spawn.pokemon.name} to database")

            maybeAwardItems(spawn.pokemon.id, alreadyDiscovered)

            val queueSnapshot = synchronized(spawnQueue) {
                spawnQueue.remove(spawn)
                spawnQueue.toList()
            }
            spawnHistory.updateActiveQueue(queueSnapshot)
            saveSpawnQueue()

            val snapshot = currentDebugSnapshot()
            debugCapture.log("catch_success", snapshot) {
                buildJsonObject {
                    put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                    put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                    put("pool", JsonPrimitive(spawn.pool.name))
                    put("caughtAt", JsonPrimitive(caughtPokemon.caughtAt))
                }
            }

            animationCoordinator.cancelActive()
            displayedSpawn = null

            showCatchAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving caught Pokémon", e)
            DebugExceptionTracker.log(applicationContext, e, currentDebugSnapshot(), "catch_pokemon")
        }
    }

    private suspend fun maybeAwardItems(speciesId: Int, wasDuplicate: Boolean) {
        if (Random.nextDouble() < EVOLUTION_STONE_DROP_CHANCE && EVOLUTION_STONES.isNotEmpty()) {
            val stone = EVOLUTION_STONES.random()
            grantItem(stone)
            logItemAward(stone, "stone", speciesId, wasDuplicate)
        }

        if (wasDuplicate) {
            grantItem(Item.RARE_CANDY)
            logItemAward(Item.RARE_CANDY, "duplicate", speciesId, wasDuplicate)
        }

        val totalCaught = db.pokemonDao().getTotalCaughtCount()
        val linkingGuaranteed = totalCaught > 0 && totalCaught % LINKING_CORD_MILESTONE == 0
        val linkingChance = Random.nextDouble() < LINKING_CORD_DROP_CHANCE
        if (linkingGuaranteed || linkingChance) {
            grantItem(Item.LINKING_CORD)
            logItemAward(
                Item.LINKING_CORD,
                if (linkingGuaranteed) "milestone" else "chance",
                speciesId,
                wasDuplicate
            )
        }
    }

    private suspend fun logItemAward(item: Item, reason: String, speciesId: Int, wasDuplicate: Boolean) {
        val snapshot = currentDebugSnapshot()
        debugCapture.log("item_award", snapshot) {
            buildJsonObject {
                put("item", JsonPrimitive(item.name.lowercase(Locale.US)))
                put("reason", JsonPrimitive(reason))
                put("speciesId", JsonPrimitive(speciesId))
                put("duplicate", JsonPrimitive(wasDuplicate))
            }
        }
    }

    private suspend fun grantItem(item: Item, amount: Int = 1) {
        val inventoryDao = db.inventoryDao()
        val existing = inventoryDao.getItem(item.ordinal)
        if (existing == null) {
            inventoryDao.insertItem(InventoryItem(itemId = item.ordinal, quantity = amount))
        } else {
            inventoryDao.addItems(item.ordinal, amount)
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
     * Acquires a wake lock to prevent animation freezes.
     */
    private fun acquireAnimationWakeLock() {
        val wakeLock = animationWakeLock ?: return
        synchronized(animationWakeLockMutex) {
            if (activeAnimationWakeLockHolders == 0) {
                val acquired = runCatching { wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS) }
                    .onFailure { error -> Log.w(TAG, "Unable to acquire animation wake lock", error) }
                    .isSuccess
                if (!acquired) {
                    return
                }
            }
            activeAnimationWakeLockHolders++
        }
    }

    /**
     * Releases the wake lock to preserve battery.
     */
    private fun releaseAnimationWakeLock() {
        val wakeLock = animationWakeLock ?: return
        synchronized(animationWakeLockMutex) {
            if (activeAnimationWakeLockHolders == 0) {
                return
            }
            activeAnimationWakeLockHolders--
            if (activeAnimationWakeLockHolders == 0 && wakeLock.isHeld) {
                runCatching { wakeLock.release() }
                    .onFailure { error -> Log.w(TAG, "Unable to release animation wake lock", error) }
            }
        }
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
                    putString(PREFS_KEY_SPAWN_QUEUE, json)
                }
            Log.d(TAG, "Saved ${persistentSpawns.size} spawns to storage")
        }
    }

    /**
     * Restores the spawn queue from SharedPreferences.
     */
    private fun restoreSpawnQueue() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(PREFS_KEY_SPAWN_QUEUE, null) ?: run {
            spawnHistory.updateActiveQueue(emptyList())
            return
        }

        var snapshot: List<SpawnResult>? = null
        synchronized(spawnQueue) {
            spawnQueue.clear()
            displayedSpawn = null

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
                snapshot = spawnQueue.toList()
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring spawn queue", e)
                prefs.edit { remove(PREFS_KEY_SPAWN_QUEUE) }
                DebugExceptionTracker.log(applicationContext, e, currentDebugSnapshot(), "restore_spawn_queue")
            }
        }

        snapshot?.let { spawnHistory.updateActiveQueue(it) }
            ?: spawnHistory.updateActiveQueue(emptyList())
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

    /**
     * Captures a game state snapshot for debug logging.
     */
    private fun currentDebugSnapshot(): DebugSnapshot = DebugSnapshot(
        phoneBattery = gameplayContext.phone.battery,
        phoneIsInteractive = gameplayContext.phone.isInteractive,
        phoneMinutesOff = gameplayContext.phone.minutesOff,
        phoneMinutesOffOutsideBedtime = gameplayContext.phone.minutesOffOutsideBedtime,
        queueSize = synchronized(spawnQueue) { spawnQueue.size },
        hasSleepBonus = gameplayContext.sleep.hasSleepBonus,
        isBedtime = gameplayContext.sleep.isBedtime
    )

    /**
     * Invoked when an unhandled exception occurs in a coroutine.
     */
    private fun handleCoroutineException(throwable: Throwable) {
        Log.e(TAG, "Unhandled coroutine exception", throwable)
        val snapshot = runCatching {
            if (::gameplayContext.isInitialized) {
                currentDebugSnapshot()
            } else {
                DebugSnapshot.EMPTY
            }
        }.getOrElse { DebugSnapshot.EMPTY }

        DebugExceptionTracker.log(
            context = applicationContext,
            throwable = throwable,
            snapshot = snapshot,
            source = "coroutine"
        )
    }

    companion object {
        private const val TAG = "PokemonGlyphToy"
        private const val MAX_QUEUE_SIZE = 3
        private const val PREFS_NAME = "pokemon_glyph_toy_prefs"
        private const val PREFS_KEY_SPAWN_QUEUE = "spawn_queue"
        private const val SLEEP_BONUS_DURATION_MILLIS = 24L * 60 * 60 * 1000
        private const val EVOLUTION_STONE_DROP_CHANCE = 0.10
        private const val LINKING_CORD_DROP_CHANCE = 0.02
        private const val LINKING_CORD_MILESTONE = 40

        private const val GLYPH_MATRIX_SIZE = 25 // 25x25 circular display
        private const val GLYPH_MATRIX_CENTER = GLYPH_MATRIX_SIZE / 2 // Center index (12, which is the 13th pixel)
        private const val WAKE_LOCK_TIMEOUT_MS = 6000L
        private const val WAKE_LOCK_TAG = "GlyphCatch:Animation"

        private const val TRAINING_EXP_PER_MINUTE = 2
        private const val TRAINING_EXP_BONUS_INTERVAL_MINUTES = 20
        private const val TRAINING_EXP_BONUS_AMOUNT = 60

        private val EVOLUTION_STONES = listOf(
            Item.FIRE_STONE,
            Item.WATER_STONE,
            Item.THUNDER_STONE,
            Item.LEAF_STONE,
            Item.MOON_STONE,
            Item.SUN_STONE
        )
    }
}
