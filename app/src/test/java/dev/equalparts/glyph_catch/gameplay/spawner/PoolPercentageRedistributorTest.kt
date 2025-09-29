package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.gameplay.spawner.models.PoolActivator
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnPool
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the pool percentage redistribution logic
 */
class PoolPercentageRedistributorTest {

    @Test
    fun `static pools maintain percentages when no dynamic pools exist`() {
        val commonPool = createPool("common", 60f)
        val uncommonPool = createPool("uncommon", 30f)
        val rarePool = createPool("rare", 10f)

        val pools = listOf(
            PoolToDistribute(commonPool, 60f),
            PoolToDistribute(uncommonPool, 30f),
            PoolToDistribute(rarePool, 10f)
        )

        val result = PoolPercentageRedistributor.redistribute(pools)

        assertEquals(60f, result[commonPool] ?: 0f, 0.01f)
        assertEquals(30f, result[uncommonPool] ?: 0f, 0.01f)
        assertEquals(10f, result[rarePool] ?: 0f, 0.01f)
    }

    @Test
    fun `dynamic pool scaling takes percentage from static pools proportionally`() {
        val commonPool = createPool("common", 60f)
        val uncommonPool = createPool("uncommon", 30f)
        val rarePool = createScalingPool("rare", 10f)

        val pools = listOf(
            PoolToDistribute(commonPool, 60f),
            PoolToDistribute(uncommonPool, 30f),
            PoolToDistribute(rarePool, 20f) // Rare is using 20% instead of base 10%
        )

        val result = PoolPercentageRedistributor.redistribute(pools)

        // Rare takes an extra 10%, static pools shrink proportionally
        // Common loses: 10 * (60/90) = 6.67
        // Uncommon loses: 10 * (30/90) = 3.33
        assertEquals(53.33f, result[commonPool] ?: 0f, 0.01f)
        assertEquals(26.67f, result[uncommonPool] ?: 0f, 0.01f)
        assertEquals(20f, result[rarePool] ?: 0f, 0.01f)
    }

    @Test
    fun `event pool with no base takes percentage from static pools`() {
        val commonPool = createPool("common", 60f)
        val uncommonPool = createPool("uncommon", 30f)
        val rarePool = createPool("rare", 10f)
        val eventPool = createEventPool("legendary", 20f)

        val pools = listOf(
            PoolToDistribute(commonPool, 60f),
            PoolToDistribute(uncommonPool, 30f),
            PoolToDistribute(rarePool, 10f),
            PoolToDistribute(eventPool, 20f)
        )

        val result = PoolPercentageRedistributor.redistribute(pools)

        // Event pool takes 20%, static pools shrink proportionally
        // Common: 60 * 0.8 = 48
        // Uncommon: 30 * 0.8 = 24
        // Rare: 10 * 0.8 = 8
        assertEquals(48f, result[commonPool] ?: 0f, 0.01f)
        assertEquals(24f, result[uncommonPool] ?: 0f, 0.01f)
        assertEquals(8f, result[rarePool] ?: 0f, 0.01f)
        assertEquals(20f, result[eventPool] ?: 0f, 0.01f)
    }

    @Test
    fun `multiple dynamic pools with sufficient static pool percentage`() {
        val commonPool = createPool("common", 77f)
        val uncommonPool = createScalingPool("uncommon", 8f)
        val eventPool = createEventPool("event", 15f)

        val pools = listOf(
            PoolToDistribute(commonPool, 77f),
            PoolToDistribute(uncommonPool, 16f), // Asking for 16%, base is 8%
            PoolToDistribute(eventPool, 15f) // Event wants 15%
        )

        val result = PoolPercentageRedistributor.redistribute(pools)

        // Dynamic pools claim extra: (16-8) + 15 = 23%
        // Common shrinks from 77% to 54%
        assertEquals(54f, result[commonPool] ?: 0f, 0.01f)
        assertEquals(16f, result[uncommonPool] ?: 0f, 0.01f)
        assertEquals(15f, result[eventPool] ?: 0f, 0.01f)

        // Total should be ~100%
        val total = result.values.sum()
        assertEquals(85f, total, 0.01f)
    }

    @Test
    fun `handles zero static pools gracefully`() {
        val eventPool1 = createEventPool("event1", 60f)
        val eventPool2 = createEventPool("event2", 40f)

        val pools = listOf(
            PoolToDistribute(eventPool1, 60f),
            PoolToDistribute(eventPool2, 40f)
        )

        val result = PoolPercentageRedistributor.redistribute(pools)

        // Both are dynamic, so they keep their percentages
        assertEquals(60f, result[eventPool1] ?: 0f, 0.01f)
        assertEquals(40f, result[eventPool2] ?: 0f, 0.01f)
    }

    @Test
    fun `normalizes dynamic pools when they exceed available static percentage`() {
        val commonPool = createPool("common", 77f)
        val uncommonPool = createScalingPool("uncommon", 20f)
        val rarePool = createScalingPool("rare", 3f)
        val eventPool = createEventPool("event", 0f)
        val eventPool2 = createEventPool("event2", 0f)

        val pools = listOf(
            PoolToDistribute(commonPool, 77f), // 77% base
            PoolToDistribute(uncommonPool, 20f), // 20% base but dynamic
            PoolToDistribute(rarePool, 3f), // 3% base but dynamic
            PoolToDistribute(eventPool, 50f), // 50% dynamic, no base
            PoolToDistribute(eventPool2, 50f) // 50% dynamic, no base
        )

        val result = PoolPercentageRedistributor.redistribute(pools)

        // Base percentages: 77 + 20 + 3 = 100%
        // Dynamic request extra: (20-20) + (3-3) + 50 + 50 = 100%
        // But only 77% static is available, so scale down

        // Common goes to 0
        assertEquals(0f, result[commonPool] ?: 0f, 0.01f)

        // Dynamic pools scale: 77/100 = 0.77
        // Event pools get: 50 * 0.77 = 38.5% each
        assertEquals(38.5f, result[eventPool] ?: 0f, 0.01f)
        assertEquals(38.5f, result[eventPool2] ?: 0f, 0.01f)

        // Uncommon/Rare keep their base (no extra requested)
        assertEquals(20f, result[uncommonPool] ?: 0f, 0.01f)
        assertEquals(3f, result[rarePool] ?: 0f, 0.01f)

        // Total should be 100%
        val total = result.values.sum()
        assertEquals(100f, total, 0.01f)
    }

    private fun createPool(name: String, basePercentage: Float): SpawnPool = SpawnPool(
        name = name,
        basePercentage = basePercentage
    )

    private fun createScalingPool(name: String, basePercentage: Float): SpawnPool = SpawnPool(
        name = name,
        basePercentage = basePercentage,
        baseModifier = { base -> base * 2 } // Scales up
    )

    private fun createEventPool(name: String, percentage: Float): SpawnPool = SpawnPool(
        name = name,
        basePercentage = 0f,
        activators = listOf(PoolActivator(percentage, { true }))
    )
}
