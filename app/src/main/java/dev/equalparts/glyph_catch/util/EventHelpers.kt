package dev.equalparts.glyph_catch.util

import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.math.abs

object EventHelpers {
    fun isHalloween(): Boolean {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return month == Calendar.OCTOBER && day in 25..31
    }

    fun isChristmas(): Boolean {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return month == Calendar.DECEMBER && day in 19..26
    }

    fun isFullMoon(): Boolean {
        val synodicMonthDays = 29.530588853
        val millisPerDay = 86_400_000.0
        val fullMoonThresholdDays = 0.9
        val referenceNewMoon = ZonedDateTime.of(2025, 9, 21, 19, 54, 0, 0, ZoneOffset.UTC)

        val nowUtc = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
        val daysSinceReference = Duration.between(referenceNewMoon, nowUtc).toMillis() / millisPerDay
        val moonAge = ((daysSinceReference % synodicMonthDays) + synodicMonthDays) % synodicMonthDays
        val delta = abs(moonAge - synodicMonthDays / 2.0)
        return delta <= fullMoonThresholdDays
    }
}
