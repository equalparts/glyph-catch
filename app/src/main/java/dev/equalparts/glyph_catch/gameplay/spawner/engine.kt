package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.gameplay.spawner.models.ModifierEffect
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnPool
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnRules
import kotlin.random.Random

/**
 * Evaluates a [SpawnRules] structure and selects Pokemon based on the current game state.
 */
class SpawnRulesEngine(val rules: SpawnRules, private val random: Random = Random.Default) {
    /**
     * Gets a map of pool names to effective percentages.
     */
    fun getCurrentPoolProbabilities(): Map<String, Float> {
        val activePools = rules.pools
            .filter { it.isActive() }
            .map { pool -> PoolToDistribute(pool, pool.getDesiredProbability()) }

        return PoolPercentageRedistributor
            .redistribute(activePools)
            .mapKeys { it.key.name }
    }

    /**
     * Gets the current spawning percentages for all Pokémon in the specified pool.
     */
    fun getPokemonProbabilities(poolName: String): Map<PokemonSpecies, Float> {
        val pool = rules.pools.find { it.name == poolName } ?: return emptyMap()
        return getPokemonProbabilities(pool)
    }

    /**
     * Pick a Pokémon to spawn based on the [SpawnRules] and current conditions.
     */
    fun spawn(screenOffDurationMinutes: Int): SpawnResult {
        val pool = selectRandomPool()
        return createSpawnResult(pool, screenOffDurationMinutes)
    }

    private fun createSpawnResult(pool: SpawnPool, screenOffDurationMinutes: Int): SpawnResult {
        val pokemon = selectRandomPokemon(pool)
        return SpawnResult(
            pokemon,
            pool,
            screenOffDurationMinutes,
            System.currentTimeMillis()
        )
    }

    /**
     * Picks a random pool using weighted random selection.
     */
    fun selectRandomPool(): SpawnPool {
        val poolProbabilities = getCurrentPoolProbabilities()

        val totalWeight = poolProbabilities.values.sum()
        var r = random.nextFloat() * totalWeight

        for ((poolName, percentage) in poolProbabilities) {
            r -= percentage
            if (r <= 0) {
                return rules.pools.find { it.name == poolName }!!
            }
        }

        throw IllegalStateException("Pool selection failed")
    }

    /**
     * Picks a random Pokémon using weighted random selection.
     */
    fun selectRandomPokemon(pool: SpawnPool): PokemonSpecies {
        val pokemonDistribution = getPokemonProbabilities(pool)

        val totalWeight = pokemonDistribution.values.sum()
        var r = random.nextFloat() * totalWeight

        for ((pokemon, probability) in pokemonDistribution) {
            r -= probability
            if (r <= 0) {
                return pokemon
            }
        }

        throw IllegalStateException("Pokemon selection failed for pool ${pool.name}")
    }

    private fun getPokemonProbabilities(pool: SpawnPool): Map<PokemonSpecies, Float> {
        val eligibleInhabitants = pool.inhabitants.filter { it.condition?.invoke() != false }
        if (eligibleInhabitants.isEmpty()) {
            return emptyMap()
        }

        val weights = mutableMapOf<PokemonSpecies, Float>()
        eligibleInhabitants.forEach { it -> weights[it.pokemon] = it.baseWeight }

        val allModifiers = pool.modifiers + rules.modifiers
        val activeEffects = allModifiers.filter { it.condition() }.flatMap { it.effects }
        activeEffects.forEach { effect ->
            when (effect) {
                is ModifierEffect.BoostType -> {
                    weights.forEach { (pokemon, weight) ->
                        if (pokemon.type1 == effect.type || pokemon.type2 == effect.type) {
                            weights[pokemon] = weight * effect.multiplier
                        }
                    }
                }
                is ModifierEffect.SuppressType -> {
                    weights.forEach { (pokemon, weight) ->
                        if (pokemon.type1 == effect.type || pokemon.type2 == effect.type) {
                            weights[pokemon] = weight * effect.multiplier
                        }
                    }
                }
                is ModifierEffect.AddPokemon -> {
                    weights[effect.pokemon] = effect.weight
                }
            }
        }

        val totalWeight = weights.values.sum()
        return if (totalWeight > 0) {
            weights.mapValues { (_, weight) -> weight / totalWeight * 100f }
        } else {
            emptyMap()
        }
    }
}

data class SpawnResult(
    val pokemon: PokemonSpecies,
    val pool: SpawnPool,
    val screenOffDurationMinutes: Int,
    val spawnedAtMillis: Long
)
