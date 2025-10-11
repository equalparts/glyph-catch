package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.data.PokemonDao
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnPool
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.runBlocking

class SpawnCadenceController(private val spawnEngine: SpawnRulesEngine, private val history: SpawnHistoryTracker) {
    private val rerollPools = spawnEngine.rules.pools.filter { it.isEligibleForReroll() }
    private val rerollPriority = rerollPools
        .sortedBy { it.basePercentage }
        .mapIndexed { index, pool -> pool.name to (rerollPools.size - index) }
        .toMap()

    private val screenOffChanceRules = listOf(
        ScreenOffChanceRule(minMinutesAccumulated = 15, baseChance = 0.03),
        ScreenOffChanceRule(minMinutesAccumulated = 30, baseChance = 0.08),
        ScreenOffChanceRule(minMinutesAccumulated = 45, baseChance = 0.12),
        ScreenOffChanceRule(minMinutesAccumulated = 60, baseChance = 1.00)
    )

    private val rerollRules = listOf(
        RerollRule(minWaitMillis = TimeUnit.HOURS.toMillis(20), rerolls = 2),
        RerollRule(minWaitMillis = TimeUnit.HOURS.toMillis(30), rerolls = 5),
        RerollRule(minWaitMillis = TimeUnit.HOURS.toMillis(40), rerolls = 50)
    )

    fun maybeSpawn(nowMillis: Long, context: SpawnContext, random: Random = Random.Default): SpawnDecision {
        if (context.isInteractive) {
            return SpawnDecision(
                null,
                SpawnDecisionDebug(
                    minutesAccumulated = 0,
                    baseChance = 0.0,
                    roll = null,
                    reason = SpawnDecisionReason.DEVICE_INTERACTIVE,
                    initialPool = null,
                    finalPool = null,
                    rerollTargetPool = null,
                    rerollsRequested = 0,
                    rerollsUsed = 0
                )
            )
        }

        val minutesAccumulated = history.computeScreenOffMinutesAccumulated(context.screenOffMinutes)
        val chance = spawnChance(context, minutesAccumulated)

        val roll = random.nextFloat()
        if (roll >= chance) {
            return SpawnDecision(
                null,
                SpawnDecisionDebug(
                    minutesAccumulated = minutesAccumulated,
                    baseChance = chance,
                    roll = roll,
                    reason = SpawnDecisionReason.ROLL_FAILED,
                    initialPool = null,
                    finalPool = null,
                    rerollTargetPool = null,
                    rerollsRequested = 0,
                    rerollsUsed = 0
                )
            )
        }

        val rerollOptions = decideRerollOptions(nowMillis, context)
        val spawnOutcome = spawnWithReroll(nowMillis, context.screenOffMinutes, rerollOptions)

        return SpawnDecision(
            spawnOutcome.result,
            SpawnDecisionDebug(
                minutesAccumulated = minutesAccumulated,
                baseChance = chance,
                roll = roll,
                reason = SpawnDecisionReason.ROLL_SUCCEEDED,
                initialPool = spawnOutcome.initialPoolName,
                finalPool = spawnOutcome.result.pool.name,
                rerollTargetPool = rerollOptions?.poolName,
                rerollsRequested = rerollOptions?.rerolls ?: 0,
                rerollsUsed = spawnOutcome.rerollsUsed
            )
        )
    }

    private fun spawnChance(context: SpawnContext, minutesAccumulated: Int): Double {
        if (context.pokedexCount == 0 && !context.hasQueuedSpawns) {
            return FIRST_CATCH_CHANCE
        }

        val baseChance = screenOffChanceRules
            .lastOrNull { minutesAccumulated >= it.minMinutesAccumulated }
            ?.baseChance
            ?: return 0.0

        return if (context.isDuringSleepWindow) {
            baseChance.coerceAtMost(MAX_SLEEP_WINDOW_CHANCE)
        } else {
            baseChance
        }
    }

    private fun spawnWithReroll(nowMillis: Long, screenOffMinutes: Int, rerollOptions: RerollOptions?): SpawnOutcome {
        val firstSpawn = spawnEngine.spawn(screenOffMinutes)

        if (rerollOptions == null || firstSpawn.pool.name == rerollOptions.poolName) {
            return SpawnOutcome(
                result = firstSpawn.copy(spawnedAtMillis = nowMillis),
                initialPoolName = firstSpawn.pool.name,
                rerollOptions = rerollOptions,
                rerollsUsed = 0
            )
        }

        var currentSpawn = firstSpawn
        var rerollsRemaining = rerollOptions.rerolls
        var rerollsUsed = 0

        while (rerollsRemaining > 0 && currentSpawn.pool.name != rerollOptions.poolName) {
            val reroll = spawnEngine.spawn(screenOffMinutes)
            currentSpawn = reroll
            rerollsRemaining--
            rerollsUsed++
        }

        return SpawnOutcome(
            result = currentSpawn.copy(spawnedAtMillis = nowMillis),
            initialPoolName = firstSpawn.pool.name,
            rerollOptions = rerollOptions,
            rerollsUsed = rerollsUsed
        )
    }

    private fun decideRerollOptions(
        nowMillis: Long,
        context: SpawnContext,
    ): RerollOptions? {
        if (rerollPools.isEmpty()
            || context.isDuringSleepWindow
            || context.screenOffMinutes < MIN_SCREEN_OFF_MINUTES_FOR_REROLL) {
            return null
        }

        var bestRerollOptions: RerollOptions? = null
        for (pool in rerollPools) {
            val lastKnownSpawnTime = history.bestKnownSpawnTime(pool.name)
            val startTime = if (lastKnownSpawnTime > 0L) {
                lastKnownSpawnTime
            } else {
                history.getPlayerStartDate()
            }

            val waitedMillis = (nowMillis - startTime).coerceAtLeast(0L)
            val rule = rerollRules.lastOrNull { waitedMillis >= it.minWaitMillis } ?: continue
            val rerollOptions = RerollOptions(
                poolName = pool.name,
                rerolls = rule.rerolls,
                priority = rerollPriority[pool.name] ?: 0,
                waitedMillis = waitedMillis
            )

            bestRerollOptions = selectBetterReroll(bestRerollOptions, rerollOptions)
        }

        return bestRerollOptions
    }

    private fun selectBetterReroll(current: RerollOptions?, candidate: RerollOptions): RerollOptions {
        current ?: return candidate
        return when {
            candidate.priority > current.priority -> candidate
            candidate.priority < current.priority -> current
            candidate.waitedMillis > current.waitedMillis -> candidate
            else -> current
        }
    }

    private fun SpawnPool.isEligibleForReroll(): Boolean = !isSpecial && !isConditional && basePercentage > 0f

    private data class SpawnOutcome(
        val result: SpawnResult,
        val initialPoolName: String,
        val rerollOptions: RerollOptions?,
        val rerollsUsed: Int
    )

    private data class ScreenOffChanceRule(val minMinutesAccumulated: Int, val baseChance: Double)
    private data class RerollRule(val minWaitMillis: Long, val rerolls: Int)
    private data class RerollOptions(val poolName: String, val rerolls: Int, val priority: Int, val waitedMillis: Long)

    companion object {
        private const val FIRST_CATCH_CHANCE = 0.2
        private const val MAX_SLEEP_WINDOW_CHANCE = 0.005
        private const val MIN_SCREEN_OFF_MINUTES_FOR_REROLL = 30
    }
}

class SpawnHistoryTracker(private val preferences: PreferencesManager, private val pokemonDao: PokemonDao) {
    var lastSpawnScreenOffMinutes: Int = preferences.lastSpawnScreenOffMinutes
        private set

    fun syncFromQueue(activeQueue: List<SpawnResult>) {
        val latest = activeQueue.maxByOrNull { it.spawnedAtMillis } ?: return
        updateLastSpawnScreenOffMinutes(latest.screenOffDurationMinutes)
        activeQueue.forEach { updatePoolTimestamp(it.pool.name, it.spawnedAtMillis) }
    }

    fun recordSpawn(spawn: SpawnResult) {
        updateLastSpawnScreenOffMinutes(spawn.screenOffDurationMinutes)
        updatePoolTimestamp(spawn.pool.name, spawn.spawnedAtMillis)
    }
    fun recordCatch(spawn: SpawnResult, caughtAtMillis: Long) {
        val timestamp = max(spawn.spawnedAtMillis, caughtAtMillis)
        updatePoolTimestamp(spawn.pool.name, timestamp)
    }

    fun bestKnownSpawnTime(poolName: String): Long {
        val fromPrefs = preferences.getLastSpawnAtForPool(poolName)
        val fromDb = loadLastCaughtAt(poolName)
        return max(fromPrefs, fromDb)
    }

    fun computeScreenOffMinutesAccumulated(currentMinutes: Int): Int =
        (currentMinutes - lastSpawnScreenOffMinutes).coerceAtLeast(0)

    fun getPlayerStartDate(): Long = preferences.playerStartDate

    private fun updateLastSpawnScreenOffMinutes(latest: Int) {
        val sanitized = latest.coerceAtLeast(0)
        lastSpawnScreenOffMinutes = sanitized
        preferences.lastSpawnScreenOffMinutes = sanitized
    }

    private fun updatePoolTimestamp(poolName: String, timestamp: Long) {
        if (timestamp <= 0L) {
            return
        }
        val stored = preferences.getLastSpawnAtForPool(poolName)
        if (timestamp > stored) {
            preferences.setLastSpawnAtForPool(poolName, timestamp)
        }
    }

    private fun loadLastCaughtAt(poolName: String): Long =
        runBlocking { pokemonDao.getLastCaughtAtForPool(poolName) } ?: 0L
}

data class SpawnContext(
    val hasQueuedSpawns: Boolean,
    val isInteractive: Boolean,
    val screenOffMinutes: Int,
    val pokedexCount: Int,
    val isDuringSleepWindow: Boolean
)

data class SpawnDecision(val spawn: SpawnResult?, val debug: SpawnDecisionDebug)

data class SpawnDecisionDebug(
    val minutesAccumulated: Int,
    val baseChance: Double,
    val roll: Float?,
    val reason: SpawnDecisionReason,
    val initialPool: String?,
    val finalPool: String?,
    val rerollTargetPool: String?,
    val rerollsRequested: Int,
    val rerollsUsed: Int
)

enum class SpawnDecisionReason {
    DEVICE_INTERACTIVE,
    ROLL_FAILED,
    ROLL_SUCCEEDED
}
