package dev.equalparts.glyph_catch.gameplay

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Gender
import dev.equalparts.glyph_catch.data.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.verify

class BreedingTest {

    @Mock
    lateinit var preferencesManager: PreferencesManager

    private lateinit var breedingController: BreedingController

    // Helper IDs for species
    private val BULBASAUR_ID = 1 // Monster/Grass
    private val VENUSAUR_ID = 3
    private val CHARMANDER_ID = 4 // Monster/Dragon
    private val SQUIRTLE_ID = 7 // Monster/Water1
    private val DITTO_ID = 132 // Ditto
    private val MEW_ID = 151 // No Eggs

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        breedingController = BreedingController(preferencesManager, null)
        
        // Default mocks for preferences
        `when`(preferencesManager.pendingEggSpeciesId).thenReturn(0)
        `when`(preferencesManager.breedingPartnerIds).thenReturn(emptySet())
        `when`(preferencesManager.breedingBeganAt).thenReturn(0L)
    }

    @Test
    fun testCompatibility_SameEggGroup_DifferentGender() {
        val maleBulbasaur = createPokemon(BULBASAUR_ID, Gender.MALE)
        val femaleCharmander = createPokemon(CHARMANDER_ID, Gender.FEMALE)
        
        assertTrue(breedingController.areCompatible(maleBulbasaur, femaleCharmander))
    }

    @Test
    fun testCompatibility_SameEggGroup_SameGender() {
        val maleBulbasaur = createPokemon(BULBASAUR_ID, Gender.MALE)
        val maleCharmander = createPokemon(CHARMANDER_ID, Gender.MALE)
        
        assertFalse(breedingController.areCompatible(maleBulbasaur, maleCharmander))
    }

    @Test
    fun testCompatibility_DifferentEggGroup() {
        // Gastly is Amorphous. Charmander is Monster/Dragon.
        val GASTLY_ID = 92
        val maleGastly = createPokemon(GASTLY_ID, Gender.MALE)
        val femaleCharmander = createPokemon(CHARMANDER_ID, Gender.FEMALE)
        
        assertFalse(breedingController.areCompatible(maleGastly, femaleCharmander))
    }

    @Test
    fun testCompatibility_Ditto() {
        val maleBulbasaur = createPokemon(BULBASAUR_ID, Gender.MALE)
        val ditto = createPokemon(DITTO_ID, Gender.GENDERLESS)
        
        assertTrue(breedingController.areCompatible(maleBulbasaur, ditto))
    }

    @Test
    fun testCompatibility_NoEggsGroup() {
        val mew = createPokemon(MEW_ID, Gender.GENDERLESS)
        val ditto = createPokemon(DITTO_ID, Gender.GENDERLESS)
        
        assertFalse(breedingController.areCompatible(mew, ditto))
    }

    @Test
    fun testEggSpecies_MotherInheritance() {
        val maleBulbasaur = createPokemon(BULBASAUR_ID, Gender.MALE)
        val femaleCharmander = createPokemon(CHARMANDER_ID, Gender.FEMALE)
        
        assertEquals(CHARMANDER_ID, breedingController.getEggSpeciesId(maleBulbasaur, femaleCharmander))
    }

    @Test
    fun testEggSpecies_DittoInheritance() {
        val maleBulbasaur = createPokemon(BULBASAUR_ID, Gender.MALE)
        val ditto = createPokemon(DITTO_ID, Gender.GENDERLESS)
        
        assertEquals(BULBASAUR_ID, breedingController.getEggSpeciesId(maleBulbasaur, ditto))
    }

    @Test
    fun testEggSpecies_BaseSpecies() {
        // Even if mother is Venusaur, egg should be Bulbasaur
        val maleCharmander = createPokemon(CHARMANDER_ID, Gender.MALE)
        val femaleVenusaur = createPokemon(VENUSAUR_ID, Gender.FEMALE)
        
        assertEquals(BULBASAUR_ID, breedingController.getEggSpeciesId(maleCharmander, femaleVenusaur))
    }

    @Test
    fun testProcessBreeding_TimeRestriction() {
        val p1 = createPokemon(BULBASAUR_ID, Gender.MALE, "p1")
        val p2 = createPokemon(CHARMANDER_ID, Gender.FEMALE, "p2")
        val partners = listOf(p1, p2)
        val partnerIds = setOf("p1", "p2")
        
        `when`(preferencesManager.breedingPartnerIds).thenReturn(partnerIds)
        
        // 1. First tick: sets breedingBeganAt
        val now = System.currentTimeMillis()
        `when`(preferencesManager.breedingBeganAt).thenReturn(0L)
        
        val result1 = breedingController.processBreeding(partners)
        assertNull(result1)
        
        // verify breedingBeganAt was set to something >= now
        verify(preferencesManager).breedingBeganAt = anyLong()
        
        // 2. Tick after 4 hours: no egg
        `when`(preferencesManager.breedingBeganAt).thenReturn(now - (4 * 3600 * 1000))
        val result2 = breedingController.processBreeding(partners)
        assertNull(result2)
    }

    @Test
    fun testProcessBreeding_PartnerChange_ResetsTimer() {
        val p1 = createPokemon(BULBASAUR_ID, Gender.MALE, "p1")
        val p2 = createPokemon(CHARMANDER_ID, Gender.FEMALE, "p2")
        val p3 = createPokemon(SQUIRTLE_ID, Gender.FEMALE, "p3")
        
        `when`(preferencesManager.breedingPartnerIds).thenReturn(setOf("p1", "p2"))
        `when`(preferencesManager.breedingBeganAt).thenReturn(123456789L)
        
        // Change partners to p1 and p3
        breedingController.processBreeding(listOf(p1, p3))
        
        // Verify breedingBeganAt was reset to 0
        verify(preferencesManager).breedingBeganAt = 0L
    }

    private fun createPokemon(speciesId: Int, gender: Gender, id: String = "test-id"): CaughtPokemon {
        return CaughtPokemon(
            id = id,
            speciesId = speciesId,
            level = 5,
            gender = gender
        )
    }
}
