package dev.equalparts.glyph_catch.data

import kotlinx.serialization.Serializable

@Serializable
data class HatchNotification(
    val speciesId: Int,
    val timestamp: Long = System.currentTimeMillis()
)
