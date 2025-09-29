package dev.equalparts.glyph_catch.gameplay.spawner.models

import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.Type

/**
 * Describes a weight modifier.
 */
data class Modifier(val condition: () -> Boolean, val effects: List<ModifierEffect>)

/**
 * The supported effects for global modifiers and pool modifiers.
 */
sealed class ModifierEffect {

    /**
     * Increases the weight of a certain Pokémon type.
     */
    data class BoostType(val type: Type, val multiplier: Float) : ModifierEffect()

    /**
     * Reduces the weight of a certain Pokémon type.
     */
    data class SuppressType(val type: Type, val multiplier: Float) : ModifierEffect()

    /**
     * Add a specific Pokémon species to the spawn pool.
     */
    data class AddPokemon(val pokemon: PokemonSpecies, val weight: Float) : ModifierEffect()
}
