package dev.equalparts.glyph_catch.data

import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

const val SUPER_ROD_DURATION_MINUTES = 60

@StringRes
fun Item.nameRes(): Int = when (this) {
    Item.FIRE_STONE -> R.string.item_fire_stone_name
    Item.WATER_STONE -> R.string.item_water_stone_name
    Item.THUNDER_STONE -> R.string.item_thunder_stone_name
    Item.LEAF_STONE -> R.string.item_leaf_stone_name
    Item.MOON_STONE -> R.string.item_moon_stone_name
    Item.SUN_STONE -> R.string.item_sun_stone_name
    Item.SUPER_ROD -> R.string.item_super_rod_name
    Item.RARE_CANDY -> R.string.item_rare_candy_name
    Item.LINKING_CORD -> R.string.item_linking_cord_name
    Item.REPEL -> R.string.item_repel_name
}

@StringRes
fun Item.descriptionRes(): Int = when (this) {
    Item.FIRE_STONE -> R.string.item_fire_stone_description
    Item.WATER_STONE -> R.string.item_water_stone_description
    Item.THUNDER_STONE -> R.string.item_thunder_stone_description
    Item.LEAF_STONE -> R.string.item_leaf_stone_description
    Item.MOON_STONE -> R.string.item_moon_stone_description
    Item.SUN_STONE -> R.string.item_sun_stone_description
    Item.SUPER_ROD -> R.string.item_super_rod_description
    Item.RARE_CANDY -> R.string.item_rare_candy_description
    Item.LINKING_CORD -> R.string.item_linking_cord_description
    Item.REPEL -> R.string.item_repel_description
}

fun Item.effectDurationMinutes(): Int? = when (this) {
    Item.SUPER_ROD -> SUPER_ROD_DURATION_MINUTES
    else -> null
}
