package dev.equalparts.glyph_catch.gameplay.spawner.models

import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.gameplay.spawner.PoolPercentageRedistributor
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnRulesEngine

/**
 * The root data structure for the [SpawnRulesEngine] to evaluate.
 */
data class SpawnRules(val pools: List<SpawnPool>, val modifiers: List<Modifier> = emptyList())

/**
 * Describes a pool of one or more Pokémon that can spawn based on certain conditions and
 * probabilities. The spawn algorithm first selects a pool based on a percentage, then
 * selects a Pokémon within that pool based on weights.
 */
data class SpawnPool(
    val name: String,
    val basePercentage: Float = 0f,
    val inhabitants: List<PoolInhabitant> = emptyList(),
    val activators: List<PoolActivator> = emptyList(),
    val modifiers: List<Modifier> = emptyList(),
    val baseModifier: ((Float) -> Float)? = null,
    val isSpecial: Boolean = false,
    val isConditional: Boolean = false
) {
    /**
     * Indicates whether this spawn pool should be considered.
     */
    fun isActive(): Boolean = basePercentage > 0f || activators.any { it.condition() }

    /**
     * Calculates the desired probability of this pool being selected by the spawn algorithm.
     *
     * Might be adjusted by the [PoolPercentageRedistributor].
     */
    fun getDesiredProbability(): Float {
        if (basePercentage == 0f) {
            val activeActivation = activators.firstOrNull { it.condition() }
            return activeActivation?.let {
                it.modifier?.invoke(it.percentage) ?: it.percentage
            } ?: 0f
        }

        return baseModifier?.invoke(basePercentage) ?: basePercentage
    }
}

/**
 * Describes the weight (and optional special condition) of a Pokémon species for a specific pool.
 */
data class PoolInhabitant(val pokemon: PokemonSpecies, val baseWeight: Float, val condition: (() -> Boolean)? = null)

/**
 * Describes an activation condition for a specific pool.
 */
data class PoolActivator(val percentage: Float, val condition: () -> Boolean, val modifier: ((Float) -> Float)? = null)
