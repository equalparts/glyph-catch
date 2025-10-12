package dev.equalparts.glyph_catch.gameplay.spawner.models

import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class SpawnScalingTest {
    @Test
    fun `time boost caps at gain`() {
        val scaling = SpawnScaling(
            minutesProvider = { 120 },
            sleepBonusProvider = { false }
        )

        val bonus = scaling.timeBoost(gain = 20f, over = 60.minutes)

        assertEquals(20f, bonus, 0.0001f)
    }

    @Test
    fun `time boost respects ramp progress`() {
        val scaling = SpawnScaling(
            minutesProvider = { 45 },
            sleepBonusProvider = { false }
        )

        val bonus = scaling.timeBoost(gain = 12f, over = 60.minutes)

        assertEquals(9f, bonus, 0.0001f)
    }

    @Test
    fun `time boost ignores minutes before start threshold`() {
        val scaling = SpawnScaling(
            minutesProvider = { 40 },
            sleepBonusProvider = { false }
        )

        val bonus = scaling.timeBoost(gain = 15f, over = 60.minutes, startAfter = 30.minutes)

        assertEquals(2.5f, bonus, 0.0001f)
    }

    @Test
    fun `time boost can use custom source minutes`() {
        val scaling = SpawnScaling(
            minutesProvider = { 0 },
            sleepBonusProvider = { false }
        )

        val bonus = scaling.timeBoost(gain = 9f, over = 90.minutes, sourceMinutes = 45)

        assertEquals(4.5f, bonus, 0.0001f)
    }

    @Test
    fun `sleep bonus only applies when active`() {
        val inactive = SpawnScaling(
            minutesProvider = { 0 },
            sleepBonusProvider = { false }
        )
        val active = SpawnScaling(
            minutesProvider = { 0 },
            sleepBonusProvider = { true }
        )

        assertEquals(0f, inactive.sleepBonus(10f), 0.0f)
        assertEquals(10f, active.sleepBonus(10f), 0.0f)
    }

    @Test
    fun `minutes property reflects provider`() {
        val scaling = SpawnScaling(
            minutesProvider = { 37 },
            sleepBonusProvider = { false }
        )

        assertEquals(37, scaling.minutes)
    }
}
