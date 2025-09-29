package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnPool

/**
 * Handles percentage redistribution when dynamic pools activate.
 *
 * When dynamic pools activate (e.g., legendary at 10%), they need to take their
 * percentage from the standard pools proportionally. This ensures the total always
 * equals 100% while the event pool gets a predictable percentage.
 *
 * Example:
 *
 * - Standard pools: Common(60%), Uncommon(30%), Rare(10%) = 100%
 * - Event activates: Legendary wants 20%
 * - Result: Common(48%), Uncommon(24%), Rare(8%), Legendary(20%) = 100%
 */
object PoolPercentageRedistributor {

    /**
     * Redistributes percentages so they add up to 100%.
     *
     * See class summary for examples.
     */
    fun redistribute(activePools: List<PoolToDistribute>): Map<SpawnPool, Float> {
        val deductiblePools = activePools.filter { (pool, _) ->
            pool.basePercentage > 0 && pool.baseModifier == null
        }

        val nonDeductiblePools = activePools.filter { (pool, _) ->
            pool.baseModifier != null || pool.activators.isNotEmpty()
        }

        val deductibleTotal = deductiblePools.sumOf { it.desiredPercentage.toDouble() }.toFloat()
        val nonDeductibleRequested = nonDeductiblePools.sumOf { it.desiredPercentage.toDouble() }.toFloat()
        val nonDeductibleBase = nonDeductiblePools.sumOf { it.pool.basePercentage.toDouble() }.toFloat()
        val extraNeeded = nonDeductibleRequested - nonDeductibleBase

        val finalPercentages = mutableMapOf<SpawnPool, Float>()

        when {
            extraNeeded <= 0 || deductibleTotal == 0f -> {
                // Everyone receives their requested percentage:
                activePools.forEach { (pool, percentage) ->
                    finalPercentages[pool] = percentage
                }
            }
            extraNeeded <= deductibleTotal -> {
                // Dynamic pools receive their requested percentage:
                nonDeductiblePools.forEach { (pool, percentage) ->
                    finalPercentages[pool] = percentage
                }

                // The rest shrinks proportionally:
                deductiblePools.forEach { activePool ->
                    val shrinkFactor = extraNeeded * (activePool.desiredPercentage / deductibleTotal)
                    finalPercentages[activePool.pool] = activePool.desiredPercentage - shrinkFactor
                }
            }
            else -> {
                val scaleFactor = deductibleTotal / extraNeeded

                // Dynamic pools receive a proportional increase:
                nonDeductiblePools.forEach { activePool ->
                    val baseAmount = activePool.pool.basePercentage
                    val extraAmount = activePool.desiredPercentage - baseAmount
                    val scaledExtra = extraAmount * scaleFactor
                    finalPercentages[activePool.pool] = baseAmount + scaledExtra
                }

                // The rest is consumed completely:
                deductiblePools.forEach { activePool ->
                    finalPercentages[activePool.pool] = 0f
                }
            }
        }

        return finalPercentages
    }
}

/**
 * Represents a spawn pool with its desired selection probability.
 */
data class PoolToDistribute(val pool: SpawnPool, val desiredPercentage: Float)
