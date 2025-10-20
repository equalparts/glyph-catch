package dev.equalparts.glyph_catch.data

import kotlinx.serialization.Serializable

@Serializable
data class EvolutionNotification(
    val previousSpeciesId: Int,
    val newSpeciesId: Int,
    val timestamp: Long = System.currentTimeMillis()
)
