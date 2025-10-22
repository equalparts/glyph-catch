package dev.equalparts.glyph_catch.gameplay

/**
 * Handles EXP math for the leveling system.
 */
object LevelCalculator {
    const val MAX_LEVEL = 100
    private const val EXP_PER_LEVEL = 300

    data class Result(val level: Int, val exp: Int, val leveledUp: Boolean)

    /**
     * Calculates the EXP needed to level up to the next level.
     */
    @Suppress("unused")
    fun expNeeded(currentLevel: Int): Int = EXP_PER_LEVEL

    /**
     * Calculates the effect of EXP gain. Returns `null` if there was no effect.
     */
    fun expResult(currentLevel: Int, currentExp: Int, gainedExp: Int): Result? {
        if (gainedExp <= 0) {
            return null
        }

        if (currentLevel >= MAX_LEVEL) {
            return null
        }

        val realCurrentExp = sanitizeExp(currentLevel, currentExp)

        val currentTotalExp = toTotalExp(currentLevel, realCurrentExp)
        val newTotalExp = (currentTotalExp + gainedExp).coerceAtMost(toTotalExp(MAX_LEVEL, 0))
        val newLevel = ((newTotalExp / EXP_PER_LEVEL).coerceAtMost(MAX_LEVEL - 1)) + 1
        val newExp = if (newLevel >= MAX_LEVEL) 0 else newTotalExp % EXP_PER_LEVEL

        val leveledUp = newLevel > currentLevel

        return if (!leveledUp && newExp == realCurrentExp) {
            null
        } else {
            Result(newLevel, newExp, leveledUp)
        }
    }

    fun progressFraction(level: Int, exp: Int): Float = if (level < MAX_LEVEL) {
        sanitizeExp(level, exp).toFloat() / EXP_PER_LEVEL.toFloat()
    } else {
        1f
    }

    private fun toTotalExp(level: Int, exp: Int): Int = (level - 1) * EXP_PER_LEVEL + sanitizeExp(level, exp)

    private fun sanitizeExp(level: Int, exp: Int): Int = when {
        level >= MAX_LEVEL -> 0
        else -> exp.coerceIn(0, EXP_PER_LEVEL - 1)
    }
}
