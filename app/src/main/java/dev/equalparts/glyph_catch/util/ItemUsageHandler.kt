package dev.equalparts.glyph_catch.util

import androidx.room.withTransaction
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ItemUsageResult {
    data class Success(val updatedPokemon: CaughtPokemon, val event: UsageEvent) : ItemUsageResult()
    data class Error(val reason: ItemUsageError, val pokemon: CaughtPokemon? = null) : ItemUsageResult()
}

sealed class UsageEvent {
    data class LevelUp(val newLevel: Int) : UsageEvent()
    data class Evolution(val previousSpeciesId: Int, val newSpeciesId: Int, val newLevel: Int) : UsageEvent()
    data class HappinessIncrease(val newHappiness: Int) : UsageEvent()
}

enum class ItemUsageError {
    ITEM_NOT_CONSUMABLE,
    ITEM_NOT_AVAILABLE,
    INVALID_POKEMON,
    ALREADY_MAX_LEVEL,
    NO_APPLICABLE_EVOLUTION
}

suspend fun useItemOnPokemon(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    item: Item,
    pokemonId: String
): ItemUsageResult = withContext(Dispatchers.IO) {
    val pokemonDao = db.pokemonDao()
    val current = pokemonDao.getCaughtPokemon(pokemonId)
        ?: return@withContext ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    when (item) {
        Item.RARE_CANDY -> applyRareCandy(db, preferencesManager, current)
        Item.SOOTHE_BELL_COOKIE -> applySootheBellCookie(db, preferencesManager, current)
        Item.LINKING_CORD -> applyEvolutionItem(db, preferencesManager, Item.LINKING_CORD, current) { pokemon ->
            findTradeEvolutionTarget(pokemon)
        }
        Item.FIRE_STONE,
        Item.WATER_STONE,
        Item.THUNDER_STONE,
        Item.LEAF_STONE,
        Item.MOON_STONE,
        Item.SUN_STONE -> applyEvolutionItem(db, preferencesManager, item, current) { pokemon ->
            findStoneEvolutionTarget(pokemon, item)
        }
        else -> ItemUsageResult.Error(ItemUsageError.ITEM_NOT_CONSUMABLE, current)
    }
}

private suspend fun applyEvolutionItem(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    item: Item,
    current: CaughtPokemon,
    targetFor: (CaughtPokemon) -> PokemonSpecies?
): ItemUsageResult = db.withTransaction {
    val pokemonDao = db.pokemonDao()
    val inventoryDao = db.inventoryDao()

    val refreshed = pokemonDao.getCaughtPokemon(current.id)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    val quantity = inventoryDao.getItem(item.ordinal)?.quantity ?: 0
    if (quantity <= 0) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ITEM_NOT_AVAILABLE, refreshed)
    }

    val target = targetFor(refreshed)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.NO_APPLICABLE_EVOLUTION, refreshed)

    inventoryDao.useItem(item.ordinal)
    pokemonDao.evolvePokemon(
        pokemonId = refreshed.id,
        newSpeciesId = target.id,
        newLevel = refreshed.level,
        newExp = refreshed.exp
    )
    pokemonDao.recordPokedexEntry(target.id)
    MilestoneHandler.checkMilestones(pokemonDao, preferencesManager)

    val updated = pokemonDao.getCaughtPokemon(refreshed.id) ?: refreshed.copy(speciesId = target.id)
    preferencesManager.enqueueEvolutionNotification(refreshed.speciesId, target.id)

    ItemUsageResult.Success(
        updatedPokemon = updated,
        event = UsageEvent.Evolution(refreshed.speciesId, target.id, updated.level)
    )
}

private suspend fun applyRareCandy(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    current: CaughtPokemon
): ItemUsageResult = db.withTransaction {
    val pokemonDao = db.pokemonDao()
    val inventoryDao = db.inventoryDao()

    val refreshed = pokemonDao.getCaughtPokemon(current.id)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    val quantity = inventoryDao.getItem(Item.RARE_CANDY.ordinal)?.quantity ?: 0
    if (quantity <= 0) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ITEM_NOT_AVAILABLE, refreshed)
    }
    if (refreshed.level >= MAX_POKEMON_LEVEL) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ALREADY_MAX_LEVEL, refreshed)
    }

    val newLevel = (refreshed.level + 1).coerceAtMost(MAX_POKEMON_LEVEL)
    val newExp = 0
    val newHappiness = (refreshed.happiness + 2).coerceAtMost(255)

    inventoryDao.useItem(Item.RARE_CANDY.ordinal)
    pokemonDao.updateTrainingProgress(refreshed.id, newExp, newLevel)
    pokemonDao.updateHappiness(refreshed.id, newHappiness)

    val evolution = findLevelEvolutionTarget(refreshed.copy(level = newLevel, exp = newExp, happiness = newHappiness))
    if (evolution != null) {
        pokemonDao.evolvePokemon(
            pokemonId = refreshed.id,
            newSpeciesId = evolution.id,
            newLevel = newLevel,
            newExp = newExp
        )
        pokemonDao.recordPokedexEntry(evolution.id)
        MilestoneHandler.checkMilestones(pokemonDao, preferencesManager)
        val updated = pokemonDao.getCaughtPokemon(refreshed.id)
            ?: refreshed.copy(speciesId = evolution.id, level = newLevel, exp = newExp, happiness = newHappiness)
        preferencesManager.enqueueEvolutionNotification(refreshed.speciesId, evolution.id)
        ItemUsageResult.Success(
            updatedPokemon = updated,
            event = UsageEvent.Evolution(refreshed.speciesId, evolution.id, newLevel)
        )
    } else {
        val updated = pokemonDao.getCaughtPokemon(refreshed.id)
            ?: refreshed.copy(level = newLevel, exp = newExp, happiness = newHappiness)
        ItemUsageResult.Success(
            updatedPokemon = updated,
            event = UsageEvent.LevelUp(newLevel)
        )
    }
}

private suspend fun applySootheBellCookie(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    current: CaughtPokemon
): ItemUsageResult = db.withTransaction {
    val pokemonDao = db.pokemonDao()
    val inventoryDao = db.inventoryDao()

    val refreshed = pokemonDao.getCaughtPokemon(current.id)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    val quantity = inventoryDao.getItem(Item.SOOTHE_BELL_COOKIE.ordinal)?.quantity ?: 0
    if (quantity <= 0) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ITEM_NOT_AVAILABLE, refreshed)
    }
    
    // Amount randomized such that 4~12 cookies max it out (255).
    // range [22, 64] gives an average of 43, 255/43 = 5.9 cookies.
    val increase = (22..64).random()
    val newHappiness = (refreshed.happiness + increase).coerceAtMost(255)

    inventoryDao.useItem(Item.SOOTHE_BELL_COOKIE.ordinal)
    pokemonDao.updateHappiness(refreshed.id, newHappiness)

    val updated = pokemonDao.getCaughtPokemon(refreshed.id) ?: refreshed.copy(happiness = newHappiness)
    
    ItemUsageResult.Success(
        updatedPokemon = updated,
        event = UsageEvent.HappinessIncrease(newHappiness)
    )
}
