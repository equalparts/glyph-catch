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
        ScreenOffChanceRule(minMinutesOff = 15, baseChance = 0.01),
        ScreenOffChanceRule(minMinutesOff = 30, baseChance = 0.02),
        ScreenOffChanceRule(minMinutesOff = 60, baseChance = 0.03)
    )

    private val rerollRules = listOf(
        RerollRule(minWaitMillis = TimeUnit.HOURS.toMillis(16), rerolls = 1),
        RerollRule(minWaitMillis = TimeUnit.HOURS.toMillis(32), rerolls = 2),
        RerollRule(minWaitMillis = TimeUnit.HOURS.toMillis(48), rerolls = 50)
    )

    fun maybeSpawn(nowMillis: Long, snapshot: SpawnSnapshot, random: Random = Random.Default): SpawnResult? {
        if (snapshot.isInteractive) {
            return null
        }

        val effectiveMinutes = history.computeEffectiveScreenOffMinutes(snapshot.screenOffMinutes)
        val chance = spawnChance(snapshot, effectiveMinutes)
        if (chance <= 0.0 || random.nextFloat() >= chance) {
            return null
        }

        return spawnWithReroll(nowMillis, snapshot.screenOffMinutes)
    }

    private fun spawnChance(snapshot: SpawnSnapshot, effectiveMinutesOff: Int): Double {
        if (snapshot.pokedexCount == 0 && !snapshot.hasQueuedSpawns) {
            return FIRST_CATCH_CHANCE
        }

        val baseChance = screenOffChanceRules
            .lastOrNull { effectiveMinutesOff >= it.minMinutesOff }
            ?.baseChance
            ?: return 0.0

        return if (snapshot.isDuringSleepWindow) {
            baseChance.coerceAtMost(MAX_SLEEP_WINDOW_CHANCE)
        } else {
            baseChance
        }
    }

    private fun spawnWithReroll(nowMillis: Long, screenOffMinutes: Int): SpawnResult? {
        var spawn = spawnEngine.spawn(screenOffMinutes) ?: return null
        val request = computeRerollRequest(nowMillis)

        if (request != null && !spawn.pool.name.equals(request.poolName, ignoreCase = true)) {
            var current = spawn
            var rerollsRemaining = request.rerolls
            while (rerollsRemaining > 0 && !current.pool.name.equals(request.poolName, ignoreCase = true)) {
                val reroll = spawnEngine.spawn(screenOffMinutes) ?: break
                current = reroll
                rerollsRemaining--
            }
            spawn = current
        }

        return spawn.copy(spawnedAtMillis = nowMillis)
    }

    private fun computeRerollRequest(nowMillis: Long): RerollRequest? {
        if (rerollPools.isEmpty()) {
            return null
        }

        var best: RerollRequest? = null
        for (pool in rerollPools) {
            val lastKnown = history.bestKnownSpawnTime(pool.name)
            if (lastKnown <= 0L) continue

            val waitedMillis = (nowMillis - lastKnown).coerceAtLeast(0L)
            val rule = rerollRules.lastOrNull { waitedMillis >= it.minWaitMillis } ?: continue
            val request = RerollRequest(
                poolName = pool.name,
                rerolls = rule.rerolls,
                priority = rerollPriority[pool.name] ?: 0,
                waitedMillis = waitedMillis
            )

            best = selectBetterReroll(best, request)
        }

        return best
    }

    private fun selectBetterReroll(current: RerollRequest?, candidate: RerollRequest): RerollRequest {
        current ?: return candidate
        return when {
            candidate.priority > current.priority -> candidate
            candidate.priority < current.priority -> current
            candidate.waitedMillis > current.waitedMillis -> candidate
            else -> current
        }
    }

    private fun SpawnPool.isEligibleForReroll(): Boolean = !isSpecial && !isConditional && basePercentage > 0f

    private data class ScreenOffChanceRule(val minMinutesOff: Int, val baseChance: Double)
    private data class RerollRule(val minWaitMillis: Long, val rerolls: Int)
    private data class RerollRequest(val poolName: String, val rerolls: Int, val priority: Int, val waitedMillis: Long)

    companion object {
        private const val FIRST_CATCH_CHANCE = 0.2
        private const val MAX_SLEEP_WINDOW_CHANCE = 0.005
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

    fun computeEffectiveScreenOffMinutes(currentMinutes: Int): Int =
        (currentMinutes - lastSpawnScreenOffMinutes).coerceAtLeast(0)

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

data class SpawnSnapshot(
    val hasQueuedSpawns: Boolean,
    val isInteractive: Boolean,
    val screenOffMinutes: Int,
    val pokedexCount: Int,
    val isDuringSleepWindow: Boolean
)
