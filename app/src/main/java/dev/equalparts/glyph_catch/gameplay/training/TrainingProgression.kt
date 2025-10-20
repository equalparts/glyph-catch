package dev.equalparts.glyph_catch.gameplay.training

/**
 * Handles EXP math for the training partner system.
 */
object TrainingProgression {
    const val MAX_LEVEL = 100
    private const val EXP_PER_LEVEL = 300

    data class Result(val level: Int, val exp: Int, val leveledUp: Boolean)

    fun expToNextLevel(level: Int): Int = EXP_PER_LEVEL

    fun applyExp(level: Int, exp: Int, gainedExp: Int): Result? {
        if (gainedExp <= 0) return null

        val (sanitizedLevel, sanitizedExp) = sanitize(level, exp)
        if (sanitizedLevel >= MAX_LEVEL) {
            return if (sanitizedLevel == level && sanitizedExp == exp) {
                null
            } else {
                Result(sanitizedLevel, sanitizedExp, leveledUp = false)
            }
        }

        val startingTotalExp = toTotalExp(sanitizedLevel, sanitizedExp)
        val maxTotalExp = toTotalExp(MAX_LEVEL, 0)
        val targetTotalExp = (startingTotalExp + gainedExp).coerceAtMost(maxTotalExp)

        val nextLevel = ((targetTotalExp / EXP_PER_LEVEL).coerceAtMost(MAX_LEVEL - 1)) + 1
        val nextExp = if (nextLevel >= MAX_LEVEL) 0 else targetTotalExp % EXP_PER_LEVEL
        val leveledUp = nextLevel > sanitizedLevel

        return if (!leveledUp && nextLevel == sanitizedLevel && nextExp == sanitizedExp) {
            null
        } else {
            Result(nextLevel, nextExp, leveledUp)
        }
    }

    fun progressFraction(level: Int, exp: Int): Float {
        val (sanitizedLevel, sanitizedExp) = sanitize(level, exp)

        if (sanitizedLevel >= MAX_LEVEL) {
            return 1f
        }
        return sanitizedExp.toFloat() / EXP_PER_LEVEL.toFloat()
    }

    private fun sanitize(level: Int, exp: Int): Pair<Int, Int> {
        val clampedLevel = level.coerceIn(1, MAX_LEVEL)
        val clampedExp = when {
            clampedLevel >= MAX_LEVEL -> 0
            else -> exp.coerceIn(0, EXP_PER_LEVEL - 1)
        }
        return clampedLevel to clampedExp
    }

    private fun toTotalExp(level: Int, exp: Int): Int {
        val (clampedLevel, clampedExp) = sanitize(level, exp)
        val base = (clampedLevel - 1) * EXP_PER_LEVEL
        return base + clampedExp
    }
}
