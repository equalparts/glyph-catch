package dev.equalparts.glyph_catch.data

import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

data class ItemMetadata(
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    val durationMinutes: Int? = null
)

fun Item.metadata(): ItemMetadata = when (this) {
    Item.FIRE_STONE -> ItemMetadata(
        nameRes = R.string.item_fire_stone_name,
        descriptionRes = R.string.item_fire_stone_description
    )
    Item.WATER_STONE -> ItemMetadata(
        nameRes = R.string.item_water_stone_name,
        descriptionRes = R.string.item_water_stone_description
    )
    Item.THUNDER_STONE -> ItemMetadata(
        nameRes = R.string.item_thunder_stone_name,
        descriptionRes = R.string.item_thunder_stone_description
    )
    Item.LEAF_STONE -> ItemMetadata(
        nameRes = R.string.item_leaf_stone_name,
        descriptionRes = R.string.item_leaf_stone_description
    )
    Item.MOON_STONE -> ItemMetadata(
        nameRes = R.string.item_moon_stone_name,
        descriptionRes = R.string.item_moon_stone_description
    )
    Item.SUN_STONE -> ItemMetadata(
        nameRes = R.string.item_sun_stone_name,
        descriptionRes = R.string.item_sun_stone_description
    )
    Item.SUPER_ROD -> ItemMetadata(
        nameRes = R.string.item_super_rod_name,
        descriptionRes = R.string.item_super_rod_description,
        durationMinutes = 60
    )
}
