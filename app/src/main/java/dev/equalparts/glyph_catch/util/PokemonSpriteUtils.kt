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
    fun getMatrixResourceId(context: Context, pokemonId: Int, variant: String? = null): Int {
        val spriteNumber = pokemonId.toString().padStart(4, '0')
        val suffix = if (variant != null) "_$variant" else ""
        return context.resources.getIdentifier("matrix_$spriteNumber$suffix", "drawable", context.packageName)
    }

    @DrawableRes
    @SuppressLint("DiscouragedApi")
    fun getSpriteResourceId(context: Context, pokemonId: Int, variant: String? = null): Int {
        val spriteName = pokemonId.toString().padStart(4, '0')
        val suffix = if (variant != null) "_$variant" else ""
        val resourceId = context.resources.getIdentifier("sprite_$spriteName$suffix", "drawable", context.packageName)
        return if (resourceId == 0) {
            context.resources.getIdentifier("sprite_0000", "drawable", context.packageName)
        } else {
            resourceId
        }
    }
}
