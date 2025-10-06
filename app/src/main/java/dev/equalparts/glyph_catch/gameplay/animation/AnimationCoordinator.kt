package dev.equalparts.glyph_catch.gameplay.animation

import com.nothing.ketchum.GlyphMatrixManager
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnResult
import kotlin.coroutines.coroutineContext
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Coordinates the playback of animations on the Glyph Matrix display.
 */
internal class AnimationCoordinator(
    private val glyphFrameHelper: GlyphMatrixHelper,
    private val glyphMatrixManagerProvider: () -> GlyphMatrixManager
) {

    private var activeAnimationJob: Job? = null
    private val circleFrameCache = mutableMapOf<Int, IntArray>()
    private val matrixSize = glyphFrameHelper.matrixSize

    val isAnimating: Boolean
        get() = activeAnimationJob?.isActive == true

    /**
     * Cancels any ongoing animations.
     */
    fun cancelActive() {
        activeAnimationJob?.cancel()
        activeAnimationJob = null
    }

    /**
     * Shows a static Pokémon image.
     */
    fun showPokemon(pokemonId: Int) {
        val frame = glyphFrameHelper.renderPokemonFrame(pokemonId)
        glyphMatrixManagerProvider().setMatrixFrame(frame)
    }

    /**
     * Played when a new Pokémon appears.
     */
    fun playSpawn(scope: CoroutineScope, spawn: SpawnResult, onDisplayed: (SpawnResult) -> Unit) {
        cancelActive()

        val job = scope.launch {
            try {
                val normalBitmap = glyphFrameHelper.getPokemonBitmap(spawn.pokemon.id)
                val normalFrame = glyphFrameHelper.renderBitmapFrame(normalBitmap)
                val invertedFrame = glyphFrameHelper.renderBitmapFrame(glyphFrameHelper.inverted(normalBitmap))

                glyphMatrixManagerProvider().setMatrixFrame(normalFrame)

                repeat(SPAWN_FLASH_COUNT) {
                    delay(SPAWN_FLASH_INTERVAL_MS)
                    glyphMatrixManagerProvider().setMatrixFrame(invertedFrame)

                    delay(SPAWN_FLASH_INTERVAL_MS)
                    glyphMatrixManagerProvider().setMatrixFrame(normalFrame)
                }

                coroutineContext.ensureActive()
                onDisplayed(spawn)
            } finally {
                clearActiveJob(coroutineContext[Job])
            }
        }

        activeAnimationJob = job
    }

    /**
     * Played when the player catches the currently visible Pokémon.
     */
    suspend fun playCatch() {
        val job = coroutineContext[Job]
        activeAnimationJob = job

        try {
            val manager = glyphMatrixManagerProvider()

            manager.setMatrixFrame(
                circleFrameCache.getOrPut(matrixSize) {
                    glyphFrameHelper.renderCircleFrame(matrixSize)
                }
            )
            delay(CATCH_INITIAL_FLASH_MS)

            for (frame in buildCircleShrinkFrames()) {
                coroutineContext.ensureActive()
                manager.setMatrixFrame(frame.frame)
                delay(frame.durationMs)
            }

            val animationFrames = glyphFrameHelper.loadCatchAnimationFrames()
            animationFrames.forEachIndexed { index, frame ->
                coroutineContext.ensureActive()
                manager.setMatrixFrame(frame)
                val frameDelay = if (index == 0) CATCH_ANIMATION_DELAY_MS else CATCH_ANIMATION_FRAME_MS
                delay(frameDelay)
            }

            manager.setMatrixFrame(animationFrames.last())
            delay(CATCH_POKEBALL_HOLD_MS)

            manager.setMatrixFrame(glyphFrameHelper.renderBlankFrame())
            delay(CATCH_POST_CLEAR_DELAY_MS)
        } finally {
            clearActiveJob(job)
        }
    }

    private fun clearActiveJob(job: Job?) {
        if (activeAnimationJob === job) {
            activeAnimationJob = null
        }
    }

    private fun buildCircleShrinkFrames(): List<AnimationFrame> {
        val totalDelta = matrixSize - CATCH_CIRCLE_DIAMETER

        val diameters = ArrayList<Int>(totalDelta)
        val durations = ArrayList<Long>(totalDelta)

        var previousPortion = 0.0
        for (step in 1..totalDelta) {
            val progress = step / totalDelta.toDouble()
            val timePortion = easeOutStrongInverse(progress)
            val deltaPortion = (timePortion - previousPortion).coerceAtLeast(0.0)
            durations += (deltaPortion * CATCH_CIRCLE_DURATION_MS).roundToLong()
            diameters += (matrixSize - step).coerceAtLeast(CATCH_CIRCLE_DIAMETER)
            previousPortion = timePortion
        }

        var adjustedTotal = 0L
        for (index in durations.indices) {
            val coerced = durations[index].coerceAtLeast(MIN_FRAME_DURATION_MS)
            durations[index] = coerced
            adjustedTotal += coerced
        }

        var difference = adjustedTotal - CATCH_CIRCLE_DURATION_MS
        var index = durations.lastIndex
        while (difference > 0 && index >= 0) {
            val reducible = (durations[index] - MIN_FRAME_DURATION_MS).coerceAtLeast(0L)
            if (reducible > 0) {
                val reduction = if (difference < reducible) difference else reducible
                durations[index] -= reduction
                difference -= reduction
            }
            index--
        }

        val frames = ArrayList<AnimationFrame>(diameters.size)
        diameters.forEachIndexed { idx, diameter ->
            val frame = circleFrameCache.getOrPut(diameter) { glyphFrameHelper.renderCircleFrame(diameter) }
            frames += AnimationFrame(frame, durations[idx].coerceAtLeast(MIN_FRAME_DURATION_MS))
        }

        return frames
    }

    private fun easeOutStrongInverse(progress: Double): Double {
        val clamped = progress.coerceIn(0.0, 1.0)
        if (clamped <= 0.0) return 0.0
        if (clamped >= 1.0) return 1.0

        val k = CATCH_SHRINK_CURVE_STRENGTH
        val base = 1 - clamped * (1 - exp(-k))
        return -ln(base) / k
    }

    private class AnimationFrame(val frame: IntArray, val durationMs: Long)

    companion object {
        private const val SPAWN_FLASH_COUNT = 2
        private const val SPAWN_FLASH_INTERVAL_MS = 100L

        private const val CATCH_INITIAL_FLASH_MS = 300L
        private const val CATCH_CIRCLE_DIAMETER = 7
        private const val CATCH_CIRCLE_DURATION_MS = 180L
        private const val CATCH_SHRINK_CURVE_STRENGTH = 14.0
        private const val MIN_FRAME_DURATION_MS = 8L

        private const val CATCH_ANIMATION_DELAY_MS = 500L
        private const val CATCH_ANIMATION_FRAME_MS = 100L
        private const val CATCH_POKEBALL_HOLD_MS = 1500L
        private const val CATCH_POST_CLEAR_DELAY_MS = 1000L
    }
}
