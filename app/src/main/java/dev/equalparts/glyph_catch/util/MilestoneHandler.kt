package dev.equalparts.glyph_catch.util

import android.util.Log
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDao
import dev.equalparts.glyph_catch.data.PreferencesManager

object MilestoneHandler {
    private const val TAG = "MilestoneHandler"

    /**
     * Checks for game milestones and awards special rewards.
     */
    suspend fun checkMilestones(
        pokemonDao: PokemonDao,
        preferencesManager: PreferencesManager
    ) {
        if (!preferencesManager.hasReceivedTogepiEggMilestone) {
            val uniqueSpeciesCount = pokemonDao.getUniqueSpeciesCount()
            if (uniqueSpeciesCount >= 20) {
                // Only award if no egg is currently pending.
                // If an egg is pending, we'll try again on the next catch/evolution/connection.
                if (preferencesManager.pendingEggSpeciesId == 0) {
                    preferencesManager.hasReceivedTogepiEggMilestone = true
                    preferencesManager.pendingEggSpeciesId = Pokemon.TOGEPI.id
                    preferencesManager.pendingEggAppearedAt = System.currentTimeMillis()
                    Log.d(TAG, "Togepi egg awarded for 20 unique species!")
                }
            }
        }
    }
}
