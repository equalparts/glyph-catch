package dev.equalparts.glyph_catch.debug

/**
 * Snapshot of device/gameplay state captured alongside each debug event.
 */
data class DebugSnapshot(
    val phoneBattery: Int,
    val phoneIsInteractive: Boolean,
    val phoneMinutesOff: Int,
    val phoneMinutesOffOutsideBedtime: Int,
    val queueSize: Int,
    val hasSleepBonus: Boolean,
    val isBedtime: Boolean
) {
    companion object {
        val EMPTY = DebugSnapshot(
            phoneBattery = 0,
            phoneIsInteractive = false,
            phoneMinutesOff = 0,
            phoneMinutesOffOutsideBedtime = 0,
            queueSize = 0,
            hasSleepBonus = false,
            isBedtime = false
        )
    }
}
