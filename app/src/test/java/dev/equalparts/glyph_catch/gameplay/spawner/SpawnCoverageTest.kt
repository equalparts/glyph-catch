package dev.equalparts.glyph_catch.gameplay.spawner

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.gameplay.spawner.models.ModifierEffect
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SpawnCoverageTest {

    private fun createMockContext(): GameplayContext {
        val mockContext = mock(Context::class.java)
        val mockBatteryManager = mock(BatteryManager::class.java)
        val mockWeatherProvider = mock(WeatherProvider::class.java)
        val preferences = InMemoryPreferences()

        `when`(mockContext.getSystemService(Context.BATTERY_SERVICE)).thenReturn(mockBatteryManager)
        `when`(mockContext.getSharedPreferences("glyph_catch_prefs", Context.MODE_PRIVATE))
            .thenReturn(preferences)
        `when`(mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).thenReturn(100)
        `when`(mockWeatherProvider.getCurrentWeather()).thenReturn(Weather.CLEAR)

        return GameplayContext(
            applicationContext = mockContext,
            weatherProvider = mockWeatherProvider,
            spawnQueue = emptyList()
        ).apply {
            phone.minutesOff = 0
            phone.battery = 100
            trainer.pokedexCount = 0
        }
    }

    @Test
    fun `all stage 1 pokemon should be included in spawn rules`() {
        val context = createMockContext()

        val allPokemon = Pokemon.all.values
        val stage1Pokemon = allPokemon.filter { it.evolutionRequirement == null }

        val spawnRules = createSpawnRules(context)
        val pokemonInSpawnRules = mutableSetOf<Int>()

        spawnRules.pools.forEach { pool ->
            pool.inhabitants.forEach { entry ->
                pokemonInSpawnRules.add(entry.pokemon.id)
            }

            pool.modifiers.forEach { modifier ->
                modifier.effects.forEach { effect ->
                    if (effect is ModifierEffect.AddPokemon) {
                        pokemonInSpawnRules.add(effect.pokemon.id)
                    }
                }
            }
        }

        spawnRules.modifiers.forEach { modifier ->
            modifier.effects.forEach { effect ->
                if (effect is ModifierEffect.AddPokemon) {
                    pokemonInSpawnRules.add(effect.pokemon.id)
                }
            }
        }

        val missingPokemon = stage1Pokemon.filter { it.id !in pokemonInSpawnRules }

        if (missingPokemon.isNotEmpty()) {
            val missingNames = missingPokemon.map { "${it.name} (#${it.id})" }
            fail(
                "The following Stage 1 Pokemon are not included in spawn rules:\n" +
                    missingNames.joinToString("\n")
            )
        }
    }

    private class InMemoryPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data

        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (data[key] as? MutableSet<String>)
                ?: defValues

        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = EditorImpl()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        private inner class EditorImpl : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = applyChange(key, value)

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
                applyChange(key, values?.toMutableSet())

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = applyChange(key, value)

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = applyChange(key, value)

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = applyChange(key, value)

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = applyChange(key, value)

            override fun remove(key: String?): SharedPreferences.Editor = applyChange(key, null, remove = true)

            override fun clear(): SharedPreferences.Editor {
                clearRequested = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearRequested) {
                    data.clear()
                }
                for ((key, value) in pending) {
                    if (value == null) {
                        data.remove(key)
                    } else {
                        data[key] = value
                    }
                }
                pending.clear()
                clearRequested = false
            }

            private fun applyChange(key: String?, value: Any?, remove: Boolean = false): SharedPreferences.Editor {
                if (key != null) {
                    if (remove) {
                        pending[key] = null
                    } else {
                        pending[key] = value
                    }
                }
                return this
            }
        }
    }
}
