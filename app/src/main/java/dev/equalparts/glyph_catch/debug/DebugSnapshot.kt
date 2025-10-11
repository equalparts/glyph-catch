package dev.equalparts.glyph_catch.debug

/**
 * Snapshot of device/gameplay state captured alongside each debug event.
 */
data class DebugSnapshot(
    val batteryPercent: Int,
    val isInteractive: Boolean,
    val minutesScreenOff: Int,
    val minutesScreenOffForSpawn: Int,
    val queueSize: Int,
    val sleepMinutesOutside: Int,
    val hasSleepBonus: Boolean,
    val isDuringSleepWindow: Boolean
)
