package dev.equalparts.glyph_catch.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes

/**
 * Helpers for dynamically getting Pokémon sprite resources.
 */
object PokemonSpriteUtils {

    @DrawableRes
    @SuppressLint("DiscouragedApi")
    fun getMatrixResourceId(context: Context, pokemonId: Int): Int {
        val spriteNumber = pokemonId.toString().padStart(4, '0')
        return context.resources.getIdentifier("matrix_$spriteNumber", "drawable", context.packageName)
    }

    @DrawableRes
    @SuppressLint("DiscouragedApi")
    fun getSpriteResourceId(context: Context, pokemonId: Int): Int {
        val spriteName = pokemonId.toString().padStart(4, '0')
        val resourceId = context.resources.getIdentifier("sprite_$spriteName", "drawable", context.packageName)
        return if (resourceId == 0) {
            context.resources.getIdentifier("sprite_0000", "drawable", context.packageName)
        } else {
            resourceId
        }
    }
}
