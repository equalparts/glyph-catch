package dev.equalparts.glyph_catch.gameplay.spawner.models

/**
 * Describes the probability mechanics for a specific pool.
 */
data class ProbabilityRule(val percentage: Float, val modifier: (Float) -> Float = { it })

/**
 * Cap the total probability at the given percentage.
 */
infix fun ProbabilityRule.upTo(maxTotal: Float): ProbabilityRule {
    val originalModifier = this.modifier
    return copy(
        modifier = { base -> minOf(originalModifier(base), maxTotal) }
    )
}

/**
 * Denotes the base probability of this pool being selected by the spawn algorithm.
 */
val Number.percent: Float get() = this.toFloat()

/**
 * Raise the probability using the given modifier function.
 */
infix fun Float.increaseBy(increase: () -> Float): ProbabilityRule = ProbabilityRule(this) { base -> base + increase() }
