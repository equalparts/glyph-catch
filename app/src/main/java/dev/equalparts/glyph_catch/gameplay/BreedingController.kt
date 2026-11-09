package dev.equalparts.glyph_catch.gameplay

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EggGroup
import dev.equalparts.glyph_catch.data.Gender
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PreferencesManager
import kotlin.random.Random

interface BreedingLogger {
    fun d(tag: String, message: String)
}

class BreedingController(
    private val preferencesManager: PreferencesManager,
    private val logger: BreedingLogger? = null
) {
    /**
     * Checks if two Pokémon are compatible for breeding.
     */
    fun areCompatible(p1: CaughtPokemon, p2: CaughtPokemon): Boolean {
        val s1 = Pokemon[p1.speciesId] ?: return false
        val s2 = Pokemon[p2.speciesId] ?: return false

        if (s1.eggGroups.contains(EggGroup.NO_EGGS) ||
            s2.eggGroups.contains(EggGroup.NO_EGGS)
        ) return false

        val isDitto1 = s1.eggGroups.contains(EggGroup.DITTO)
        val isDitto2 = s2.eggGroups.contains(EggGroup.DITTO)

        if (isDitto1 && isDitto2) return false
        if (isDitto1 || isDitto2) return true

        if (p1.gender == Gender.GENDERLESS || p2.gender == Gender.GENDERLESS) return false
        if (p1.gender == p2.gender) return false

        return s1.eggGroups.any { it in s2.eggGroups }
    }

    /**
     * Determines the species ID of the egg produced by a pair.
     */
    fun getEggSpeciesId(p1: CaughtPokemon, p2: CaughtPokemon): Int {
        val s1 = Pokemon[p1.speciesId]!!
        val s2 = Pokemon[p2.speciesId]!!

        val speciesId = when {
            s1.eggGroups.contains(EggGroup.DITTO) -> s2.id
            s2.eggGroups.contains(EggGroup.DITTO) -> s1.id
            p1.gender == Gender.FEMALE -> s1.id
            else -> s2.id
        }
        return getBaseSpeciesId(speciesId)
    }

    /**
     * Recursively finds the first stage of an evolution line.
     */
    fun getBaseSpeciesId(speciesId: Int): Int {
        var current = speciesId
        while (true) {
            val parent = Pokemon.all.values.find { current in it.evolvesTo }
            if (parent == null) break
            current = parent.id
        }
        return current
    }

    /**
     * Evaluates breeding progress and potentially produces an egg.
     */
    fun processBreeding(partners: List<CaughtPokemon>): Int? {
        val currentPartnerIds = partners.map { it.id }.toSet()

        if (currentPartnerIds != preferencesManager.breedingPartnerIds) {
            preferencesManager.breedingPartnerIds = currentPartnerIds
            preferencesManager.breedingBeganAt = 0L
            logger?.d(TAG, "Training partners changed, breeding timer reset.")
        }

        if (preferencesManager.pendingEggSpeciesId != 0) return null
        if (partners.size != 2) return null

        val p1 = partners[0]
        val p2 = partners[1]

        if (!areCompatible(p1, p2)) {
            if (preferencesManager.breedingBeganAt != 0L) {
                preferencesManager.breedingBeganAt = 0L
            }
            return null
        }

        val now = System.currentTimeMillis()
        if (preferencesManager.breedingBeganAt == 0L) {
            preferencesManager.breedingBeganAt = now
            return null
        }

        val durationMillis = now - preferencesManager.breedingBeganAt
        val hoursPassed = durationMillis / (3600 * 1000)

        if (hoursPassed >= 8) {
            val extraHours = hoursPassed - 8
            val hourlyChance = (0.05 + (extraHours * 0.02)).coerceAtMost(0.5)
            val perMinuteChance = hourlyChance / 60.0

            if (Random.nextDouble() < perMinuteChance) {
                val eggSpeciesId = getEggSpeciesId(p1, p2)
                preferencesManager.pendingEggSpeciesId = eggSpeciesId
                preferencesManager.pendingEggAppearedAt = System.currentTimeMillis()
                logger?.d(TAG, "An egg has appeared! Species ID: $eggSpeciesId")
                return eggSpeciesId
            }
        }
        return null
    }

    companion object {
        private const val TAG = "BreedingController"
    }
}
