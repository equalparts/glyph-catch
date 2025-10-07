package dev.equalparts.glyph_catch.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveItemStatus(val isActive: Boolean, val remainingMinutes: Int)

@Composable
fun rememberActiveItemStatus(db: PokemonDatabase, item: Item): State<ActiveItemStatus> =
    produceState(initialValue = ActiveItemStatus(isActive = false, remainingMinutes = 0), db, item) {
        val dao = db.activeItemDao()
        var currentExpiresAt = 0L

        fun calculateStatus(expiresAt: Long): ActiveItemStatus {
            val now = System.currentTimeMillis()
            return if (expiresAt > now) {
                val remainingMillis = (expiresAt - now).coerceAtLeast(0L)
                val remainingMinutes = ceil(remainingMillis / 60_000.0).toInt()
                ActiveItemStatus(isActive = true, remainingMinutes = remainingMinutes)
            } else {
                ActiveItemStatus(isActive = false, remainingMinutes = 0)
            }
        }

        launch {
            dao.watchActiveItem(item.ordinal).collect { activeItem ->
                withContext(Dispatchers.IO) {
                    dao.cleanupExpiredItems(System.currentTimeMillis())
                }
                currentExpiresAt = activeItem?.expiresAt ?: 0L
                value = calculateStatus(currentExpiresAt)
            }
        }

        launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val delayMillis = if (currentExpiresAt <= now) {
                    60_000L
                } else {
                    (currentExpiresAt - now).coerceIn(1_000L, 30_000L)
                }
                delay(delayMillis)
                value = calculateStatus(currentExpiresAt)
            }
        }
    }

@Composable
fun rememberSleepBonusStatus(preferencesManager: PreferencesManager): State<Boolean> {
    val statusFlow = remember(preferencesManager) { preferencesManager.watchSleepBonusStatus() }
    return produceState(initialValue = preferencesManager.isSleepBonusActive(), statusFlow) {
        statusFlow.collect { isActive ->
            value = isActive
        }
    }
}
