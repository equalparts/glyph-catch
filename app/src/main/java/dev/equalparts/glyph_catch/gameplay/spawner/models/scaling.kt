package dev.equalparts.glyph_catch.gameplay.spawner.models

import dev.equalparts.glyph_catch.gameplay.spawner.GameplayContext
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

class SpawnScaling(private val sleepMinutesProvider: () -> Int, private val sleepBonusProvider: () -> Boolean) {
    constructor(context: GameplayContext) : this(
        sleepMinutesProvider = { context.sleep.minutesOutsideSleep },
        sleepBonusProvider = { context.sleep.hasSleepBonus }
    )

    val minutes: Int get() = sleepMinutesProvider()
    val minutesOff: Int get() = minutes

    fun timeBoost(gain: Float, over: Duration, startAfter: Duration = ZERO, sourceMinutes: Int = minutes): Float {
        val gainValue = gain.coerceAtLeast(0f)
        if (gainValue == 0f) return 0f

        val rampMinutes = over.inWholeMinutes.toInt()
        if (rampMinutes <= 0) return gainValue

        val startMinutes = startAfter.inWholeMinutes.toInt().coerceAtLeast(0)
        val effectiveMinutes = (sourceMinutes - startMinutes).coerceAtLeast(0)
        val progress = effectiveMinutes / rampMinutes.toFloat()
        return min(gainValue, gainValue * progress)
    }

    fun sleepBonus(bonus: Float): Float = if (sleepBonusProvider()) bonus else 0f
}
