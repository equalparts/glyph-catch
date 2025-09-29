package dev.equalparts.glyph_catch.util

import android.content.Context
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/**
 * Provides daily trainer tips from a JSON resource file.
 */
class TrainerTipsProvider(private val context: Context, private val preferencesManager: PreferencesManager) {

    private val tips: List<String> by lazy {
        loadTips()
    }

    fun getDailyTip(): String {
        val playerStartDate = preferencesManager.playerStartDate
        val startMillis = if (playerStartDate == 0L) {
            System.currentTimeMillis()
        } else {
            playerStartDate
        }

        val currentMillis = System.currentTimeMillis()
        val daysSinceStart = TimeUnit.MILLISECONDS.toDays(currentMillis - startMillis)

        val index = daysSinceStart.toInt() % tips.size
        return tips[index]
    }

    private fun loadTips(): List<String> {
        val jsonString = context.resources.openRawResource(R.raw.trainer_tips)
            .bufferedReader()
            .use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val tipsArray = jsonObject.getJSONArray("tips")

        return buildList {
            for (i in 0 until tipsArray.length()) {
                add(tipsArray.getString(i))
            }
        }
    }
}
