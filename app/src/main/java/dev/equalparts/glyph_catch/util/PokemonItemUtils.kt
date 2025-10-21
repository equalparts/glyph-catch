package dev.equalparts.glyph_catch.util

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonSpecies

const val MAX_POKEMON_LEVEL = 100

private fun PokemonSpecies.evolutionTargets(): List<PokemonSpecies> = evolvesTo.mapNotNull { Pokemon[it] }

fun findLevelEvolutionTarget(pokemon: CaughtPokemon): PokemonSpecies? {
    val species = Pokemon[pokemon.speciesId] ?: return null
    return species.evolutionTargets().firstOrNull { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Level
        requirement != null && pokemon.level >= requirement.level
    }
}

fun findStoneEvolutionTarget(pokemon: CaughtPokemon, stone: Item): PokemonSpecies? {
    val species = Pokemon[pokemon.speciesId] ?: return null
    return species.evolutionTargets().firstOrNull { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Stone
        requirement?.item == stone
    }
}

fun findTradeEvolutionTarget(pokemon: CaughtPokemon): PokemonSpecies? {
    val species = Pokemon[pokemon.speciesId] ?: return null
    return species.evolutionTargets().firstOrNull { candidate ->
        candidate.evolutionRequirement == EvolutionRequirement.Trade
    }
}

fun canUseItemOn(item: Item, pokemon: CaughtPokemon): Boolean = when (item) {
    Item.RARE_CANDY -> pokemon.level < MAX_POKEMON_LEVEL
    Item.LINKING_CORD -> findTradeEvolutionTarget(pokemon) != null
    Item.FIRE_STONE,
    Item.WATER_STONE,
    Item.THUNDER_STONE,
    Item.LEAF_STONE,
    Item.MOON_STONE,
    Item.SUN_STONE -> findStoneEvolutionTarget(pokemon, item) != null
    else -> false
}
