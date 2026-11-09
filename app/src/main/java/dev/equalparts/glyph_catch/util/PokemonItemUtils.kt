package dev.equalparts.glyph_catch.util

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.TimeOfDay
import java.util.Calendar

const val MAX_POKEMON_LEVEL = 100

private fun isDay(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour in 6..17
}

private fun isNight(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour < 6 || hour >= 20
}

private fun PokemonSpecies.evolutionTargets(): List<PokemonSpecies> = evolvesTo.mapNotNull { Pokemon[it] }

fun findLevelEvolutionTarget(pokemon: CaughtPokemon): PokemonSpecies? {
    val species = Pokemon[pokemon.speciesId] ?: return null
    val validTargets = species.evolutionTargets().filter { candidate ->
        when (val requirement = candidate.evolutionRequirement) {
            is EvolutionRequirement.Level -> pokemon.level >= requirement.level
            is EvolutionRequirement.Happiness -> pokemon.happiness >= 255
            is EvolutionRequirement.TimeHappiness -> {
                pokemon.happiness >= 255 && when (requirement.time) {
                    TimeOfDay.DAY -> isDay()
                    TimeOfDay.NIGHT -> isNight()
                }
            }
            else -> false
        }
    }
    return if (validTargets.isNotEmpty()) validTargets.random() else null
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

fun canUseItemOn(item: Item, pokemon: CaughtPokemon): Boolean {
    if (pokemon.isEgg) return false
    return when (item) {
        Item.RARE_CANDY -> pokemon.level < MAX_POKEMON_LEVEL
        Item.SOOTHE_BELL_COOKIE -> pokemon.happiness < 255
        Item.LINKING_CORD -> findTradeEvolutionTarget(pokemon) != null
        Item.FIRE_STONE,
        Item.WATER_STONE,
        Item.THUNDER_STONE,
        Item.LEAF_STONE,
        Item.MOON_STONE,
        Item.SUN_STONE -> findStoneEvolutionTarget(pokemon, item) != null
        else -> false
    }
}
