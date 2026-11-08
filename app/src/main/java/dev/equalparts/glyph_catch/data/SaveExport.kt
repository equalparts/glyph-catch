package dev.equalparts.glyph_catch.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Exports the player's progress (Pokémon, Pokédex, Bag and settings) as a save file that the
 * continued Glyph Catch at github.com/ErikPeTo1/glyph-catch can import, so moving over doesn't
 * mean starting again.
 */
object SaveExport {
    private const val FORMAT = "glyph-catch-save"
    private const val VERSION = 1
    private const val PREFS_NAME = "glyph_catch_prefs"

    suspend fun export(context: Context): String = withContext(Dispatchers.IO) {
        val db = PokemonDatabase.getInstance(context)
        val pokemon = db.pokemonDao().watchAllCaught().first()
        val pokedex = db.pokemonDao().watchCaughtSpeciesIds().first()
        val inventory = db.inventoryDao().watchAllItems().first()
        val activeItems = db.activeItemDao().getAllActiveItems()

        buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            putJsonArray("pokemon") {
                pokemon.forEach { caught ->
                    addJsonObject {
                        put("id", caught.id)
                        put("speciesId", caught.speciesId)
                        put("caughtAt", caught.caughtAt)
                        put("spawnedAt", caught.spawnedAt)
                        put("nickname", caught.nickname)
                        put("isFavorite", caught.isFavorite)
                        put("isTraining", caught.isTraining)
                        put("exp", caught.exp)
                        put("level", caught.level)
                        put("spawnPoolName", caught.spawnPoolName)
                        put("isSpecialSpawn", caught.isSpecialSpawn)
                        put("isConditionalSpawn", caught.isConditionalSpawn)
                        put("screenOffDurationMinutes", caught.screenOffDurationMinutes)
                    }
                }
            }
            putJsonArray("pokedex") { pokedex.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("inventory") {
                inventory.forEach { item ->
                    addJsonObject {
                        put("itemId", item.itemId)
                        put("quantity", item.quantity)
                    }
                }
            }
            putJsonArray("activeItems") {
                activeItems.forEach { item ->
                    addJsonObject {
                        put("itemId", item.itemId)
                        put("activatedAt", item.activatedAt)
                        put("expiresAt", item.expiresAt)
                    }
                }
            }
            putJsonObject("preferences") { exportPreferences(context).forEach { (key, value) -> put(key, value) } }
        }.toString()
    }

    private fun exportPreferences(context: Context): Map<String, JsonObject> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) ->
            val (type, primitive) = when (value) {
                is Boolean -> "boolean" to JsonPrimitive(value)
                is Int -> "int" to JsonPrimitive(value)
                is Long -> "long" to JsonPrimitive(value)
                is Float -> "float" to JsonPrimitive(value)
                is String -> "string" to JsonPrimitive(value)
                else -> return@mapNotNull null
            }
            key to buildJsonObject {
                put("type", type)
                put("value", primitive)
            }
        }.toMap()
    }
}
