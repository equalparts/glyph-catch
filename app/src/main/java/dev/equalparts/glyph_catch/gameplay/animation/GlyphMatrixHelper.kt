package dev.equalparts.glyph_catch.gameplay.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils

/**
 * Provides various utility methods for drawing frames for the Glyph Matrix.
 */
internal class GlyphMatrixHelper(private val context: Context, val matrixSize: Int) {
    private val matrixCenter = matrixSize / 2

    /**
     * Load a Pokémon sprite as a [Bitmap].
     */
    fun getPokemonBitmap(pokemonId: Int): Bitmap {
        val resourceId = PokemonSpriteUtils.getSpriteResourceId(context, pokemonId)
        return loadBitmap(resourceId)
    }

    /**
     * Render a Pokémon sprite to display on the Glyph Matrix.
     */
    fun renderPokemonFrame(pokemonId: Int, invertColors: Boolean = false): IntArray {
        val base = getPokemonBitmap(pokemonId)
        val bitmap = if (invertColors) inverted(base) else base
        return renderBitmapFrame(bitmap)
    }

    /**
     * Render a [Bitmap] to display on the Glyph Matrix.
     */
    fun renderBitmapFrame(bitmap: Bitmap): IntArray {
        val sprite = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .build()

        return GlyphMatrixFrame.Builder()
            .addTop(sprite)
            .build(context)
            .render()
    }

    /**
     * Fill a circle for display on the Glyph Matrix.
     */
    fun renderCircleFrame(diameter: Int, color: Int = Color.WHITE): IntArray =
        renderBitmapFrame(createCircleBitmap(diameter, color))

    /**
     * Render a blank frame to display on the Glyph Matrix.
     */
    fun renderBlankFrame(): IntArray = IntArray(matrixSize * matrixSize)

    /**
     * Load the Pokémon catch animation frames.
     */
    fun loadCatchAnimationFrames(): List<IntArray> {
        val frameResources = intArrayOf(
            R.drawable.catch_frame_0,
            R.drawable.catch_frame_1,
            R.drawable.catch_frame_2,
            R.drawable.catch_frame_3,
            R.drawable.catch_frame_4,
            R.drawable.catch_frame_5,
            R.drawable.catch_frame_6,
            R.drawable.catch_frame_7
        )

        return frameResources.map { renderBitmapFrame(loadBitmap(it)) }
    }

    /**
     * Helper for inverting a [Bitmap].
     */
    fun inverted(bitmap: Bitmap): Bitmap {
        val inverted = createBitmap(bitmap.width, bitmap.height)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = bitmap[x, y]
                val alpha = Color.alpha(color)
                inverted[x, y] = if (alpha == 0) {
                    color
                } else {
                    Color.argb(
                        alpha,
                        255 - Color.red(color),
                        255 - Color.green(color),
                        255 - Color.blue(color)
                    )
                }
            }
        }
        return inverted
    }

    data class SpriteFrames(val primaryFrame: IntArray, val flashFrame: IntArray)

    fun createSpawnFrames(pokemonId: Int): SpriteFrames {
        val primary = renderPokemonFrame(pokemonId)
        val flash = renderPokemonFrame(pokemonId, invertColors = true)
        return SpriteFrames(primaryFrame = primary, flashFrame = flash)
    }

    fun adjustBrightness(frame: IntArray, factor: Float): IntArray {
        if (factor >= 0.999f) {
            return frame
        }
        val result = IntArray(frame.size)
        for (index in frame.indices) {
            val color = frame[index]
            val alpha = Color.alpha(color)
            val red = (Color.red(color) * factor).toInt().coerceIn(0, 255)
            val green = (Color.green(color) * factor).toInt().coerceIn(0, 255)
            val blue = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
            result[index] = Color.argb(alpha, red, green, blue)
        }
        return result
    }

    /**
     * Loads a [Bitmap] from a resource ID using the Glyph Matrix SDK.
     */
    private fun loadBitmap(@DrawableRes resourceId: Int): Bitmap {
        val drawable = context.resources.getDrawable(resourceId, null)
        val base = GlyphMatrixUtils.drawableToBitmap(drawable)
        return base.copy(Bitmap.Config.ARGB_8888, true) ?: base
    }

    /**
     * Fills a circle on a [Bitmap] sized for the Glyph Matrix display.
     */
    private fun createCircleBitmap(diameter: Int, color: Int): Bitmap {
        val bitmap = createBitmap(matrixSize, matrixSize)
        val radius = diameter / 2.0
        for (x in 0 until matrixSize) {
            for (y in 0 until matrixSize) {
                val dx = x - matrixCenter
                val dy = y - matrixCenter
                if (dx * dx + dy * dy <= radius * radius) {
                    bitmap[x, y] = color
                }
            }
        }
        return bitmap
    }
}
