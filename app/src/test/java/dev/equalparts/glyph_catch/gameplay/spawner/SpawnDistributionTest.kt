package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.gameplay.spawner.models.increaseBy
import dev.equalparts.glyph_catch.gameplay.spawner.models.percent
import org.junit.Assert.*
import org.junit.Test

class SpawnDistributionTest {

    data class TestContext(
        var night: Boolean = false,
        var rain: Boolean = false,
        var battery: Int = 100,
        var faceDownMinutes: Int = 0
    )

    @Test
    fun `base pool probabilities match configured percentages`() {
        val context = TestContext()

        val rules = PokemonSpawnDsl(context).pools {
            pool("common", 60.percent) {
                Pokemon.PIDGEY at 1.0f
                Pokemon.RATTATA at 1.0f
            }

            pool("uncommon", 30.percent) {
                Pokemon.PIKACHU at 1.0f
            }

            pool("rare", 10.percent) {
                Pokemon.DRATINI at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)
        val poolProbabilities = engine.getCurrentPoolProbabilities()

        assertEquals(60f, poolProbabilities["common"] ?: 0f, 0.01f)
        assertEquals(30f, poolProbabilities["uncommon"] ?: 0f, 0.01f)
        assertEquals(10f, poolProbabilities["rare"] ?: 0f, 0.01f)
    }

    @Test
    fun `rare pool scales with time correctly`() {
        val context = TestContext(faceDownMinutes = 30)
        val faceDownMinutes = context::faceDownMinutes

        val rules = PokemonSpawnDsl(context).pools {
            pool("common", 60.percent) {
                Pokemon.PIDGEY at 1.0f
            }

            pool("uncommon", 27.percent) {
                Pokemon.PIKACHU at 1.0f
            }

            pool("rare", 8.percent increaseBy { faceDownMinutes.get() / 30.0f * 8.0f }) {
                Pokemon.DRATINI at 1.0f
            }

            pool("starter", 5.percent) {
                Pokemon.BULBASAUR at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)
        val poolProbabilities = engine.getCurrentPoolProbabilities()

        // At t=30, rare pool should scale from 8% to 16%
        // Common should shrink from 60% to ~54.78%
        assertEquals(54.78f, poolProbabilities["common"] ?: 0f, 0.01f)
        assertEquals(24.65f, poolProbabilities["uncommon"] ?: 0f, 0.01f)
        assertEquals(16.0f, poolProbabilities["rare"] ?: 0f, 0.01f)
        assertEquals(4.57f, poolProbabilities["starter"] ?: 0f, 0.01f)
    }

    @Test
    fun `event pool activates and takes percentage from others`() {
        val context = TestContext(battery = 5)
        val battery = context::battery

        val rules = PokemonSpawnDsl(context).pools {
            pool("common", 70.percent) {
                Pokemon.PIDGEY at 1.0f
            }

            pool("rare", 30.percent) {
                Pokemon.DRATINI at 1.0f
            }

            pool("low_battery") {
                activate(20.percent) given { battery.get() < 10 }

                Pokemon.VOLTORB at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)
        val poolProbabilities = engine.getCurrentPoolProbabilities()

        // Low battery pool should activate at 20%
        // Static pools shrink proportionally
        assertEquals(56.0f, poolProbabilities["common"] ?: 0f, 0.01f)
        assertEquals(24.0f, poolProbabilities["rare"] ?: 0f, 0.01f)
        assertEquals(20.0f, poolProbabilities["low_battery"] ?: 0f, 0.01f)
    }

    @Test
    fun `weather modifiers affect pokemon probabilities within pool`() {
        val context = TestContext(rain = true)
        val rain = context::rain

        val rules = PokemonSpawnDsl(context).pools {
            pool("main", 100.percent) {
                Pokemon.CHARMANDER at 1.0f // Fire
                Pokemon.SQUIRTLE at 1.0f // Water
                Pokemon.BULBASAUR at 1.0f // Grass/Poison

                during(rain) {
                    boost(Type.WATER) by 3.0f
                    suppress(Type.FIRE) by 0.3f
                }
            }
        }

        val engine = SpawnRulesEngine(rules)
        val pokemonProbabilities = engine.getPokemonProbabilities("main")

        // In rain, water should be boosted 3x, fire suppressed to 0.3x
        // Total weights: 0.3 + 3.0 + 1.0 = 4.3
        assertEquals(6.98f, pokemonProbabilities[Pokemon.CHARMANDER] ?: 0f, 0.01f) // 0.3/4.3 * 100
        assertEquals(69.77f, pokemonProbabilities[Pokemon.SQUIRTLE] ?: 0f, 0.01f) // 3.0/4.3 * 100
        assertEquals(23.26f, pokemonProbabilities[Pokemon.BULBASAUR] ?: 0f, 0.01f) // 1.0/4.3 * 100
    }

    @Test
    fun `full spawn probability table combines pool and pokemon probabilities`() {
        val context = TestContext()

        val rules = PokemonSpawnDsl(context).pools {
            pool("common", 60.percent) {
                Pokemon.PIDGEY at 1.0f
                Pokemon.RATTATA at 1.0f
            }

            pool("uncommon", 30.percent) {
                Pokemon.PIKACHU at 1.0f
            }

            pool("rare", 10.percent) {
                Pokemon.DRATINI at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)

        // Verify pool probabilities
        val poolProbabilities = engine.getCurrentPoolProbabilities()
        assertEquals(60f, poolProbabilities["common"] ?: 0f, 0.01f)
        assertEquals(30f, poolProbabilities["uncommon"] ?: 0f, 0.01f)
        assertEquals(10f, poolProbabilities["rare"] ?: 0f, 0.01f)

        // Verify Pokemon distribution within pools
        val commonPokemon = engine.getPokemonProbabilities("common")
        assertEquals(50f, commonPokemon[Pokemon.PIDGEY] ?: 0f, 0.01f)
        assertEquals(50f, commonPokemon[Pokemon.RATTATA] ?: 0f, 0.01f)

        val uncommonPokemon = engine.getPokemonProbabilities("uncommon")
        assertEquals(100f, uncommonPokemon[Pokemon.PIKACHU] ?: 0f, 0.01f)

        val rarePokemon = engine.getPokemonProbabilities("rare")
        assertEquals(100f, rarePokemon[Pokemon.DRATINI] ?: 0f, 0.01f)
    }

    @Test
    fun `composable conditions combine with AND logic`() {
        val context = TestContext(night = true, rain = false)
        val night = context::night
        val rain = context::rain

        val rules = PokemonSpawnDsl(context).pools {
            pool("test", 100.percent) {
                Pokemon.GASTLY at 1.0f during night given { rain.get() }
                Pokemon.HAUNTER at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)
        val pokemonProbabilities = engine.getPokemonProbabilities("test")

        // Gastly requires both night AND rain, but it's not raining
        assertEquals(0f, pokemonProbabilities[Pokemon.GASTLY] ?: 0f, 0.01f)
        assertEquals(100f, pokemonProbabilities[Pokemon.HAUNTER] ?: 0f, 0.01f)

        // Now make it rain
        context.rain = true
        val engine2 = SpawnRulesEngine(rules)
        val probsWithRain = engine2.getPokemonProbabilities("test")

        // Now both conditions are true
        assertEquals(50f, probsWithRain[Pokemon.GASTLY] ?: 0f, 0.01f)
        assertEquals(50f, probsWithRain[Pokemon.HAUNTER] ?: 0f, 0.01f)
    }

    @Test
    fun `special function creates single-pokemon event pool with it reference`() {
        // Extended test context with notCaught functionality
        data class AdvancedTestContext(
            var thunderstorm: Boolean = false,
            val caughtPokemon: MutableSet<Int> = mutableSetOf()
        ) {
            fun notCaught(species: PokemonSpecies): Boolean = !caughtPokemon.contains(species.id)
        }

        val context = AdvancedTestContext(thunderstorm = true)
        val thunderstorm = context::thunderstorm

        val rules = PokemonSpawnDsl(context).pools {
            pool("common", 100.percent) {
                Pokemon.PIDGEY at 1.0f
            }

            special(Pokemon.ZAPDOS) {
                activate(100.percent) during thunderstorm given { context.notCaught(it) }
                activate(10.percent) during thunderstorm
            }
        }

        val engine = SpawnRulesEngine(rules)

        // Verify special pool was created
        assertEquals(2, rules.pools.size)
        val specialPool = rules.pools.find { it.name == "special:zapdos" }
        assertNotNull(specialPool)

        // During thunderstorm with uncaught Zapdos - should be 100% spawn
        val poolProbs = engine.getCurrentPoolProbabilities()
        assertEquals(0f, poolProbs["common"] ?: 0f, 0.01f)
        assertEquals(100f, poolProbs["special:zapdos"] ?: 0f, 0.01f)

        // Verify Zapdos is in the special pool
        val specialPokemon = engine.getPokemonProbabilities("special:zapdos")
        assertEquals(100f, specialPokemon[Pokemon.ZAPDOS] ?: 0f, 0.01f)

        // Mark Zapdos as caught
        context.caughtPokemon.add(Pokemon.ZAPDOS.id)
        val engine2 = SpawnRulesEngine(rules)

        // Now the special pool should activate at 10% (second activation condition)
        val poolProbsAfterCatch = engine2.getCurrentPoolProbabilities()
        assertEquals(90f, poolProbsAfterCatch["common"] ?: 0f, 0.01f)
        assertEquals(10f, poolProbsAfterCatch["special:zapdos"] ?: 0f, 0.01f)
    }

    @Test
    fun `add pokemon modifier dynamically adds pokemon to pool`() {
        val context = TestContext(rain = true)
        val rain = context::rain

        val rules = PokemonSpawnDsl(context).pools {
            pool("water", 100.percent) {
                Pokemon.SQUIRTLE at 1.0f
                Pokemon.PSYDUCK at 1.0f

                during(rain) {
                    add(Pokemon.LAPRAS) at 3.0f
                }
            }
        }

        val engine = SpawnRulesEngine(rules)
        val pokemonProbabilities = engine.getPokemonProbabilities("water")

        // With rain, Lapras should be added with weight 3.0
        // Total weights: 1 + 1 + 3 = 5
        assertEquals(20f, pokemonProbabilities[Pokemon.SQUIRTLE] ?: 0f, 0.01f)
        assertEquals(20f, pokemonProbabilities[Pokemon.PSYDUCK] ?: 0f, 0.01f)
        assertEquals(60f, pokemonProbabilities[Pokemon.LAPRAS] ?: 0f, 0.01f)

        // Without rain, Lapras should not appear
        context.rain = false
        val engine2 = SpawnRulesEngine(rules)
        val probsNoRain = engine2.getPokemonProbabilities("water")

        assertEquals(50f, probsNoRain[Pokemon.SQUIRTLE] ?: 0f, 0.01f)
        assertEquals(50f, probsNoRain[Pokemon.PSYDUCK] ?: 0f, 0.01f)
        assertEquals(0f, probsNoRain[Pokemon.LAPRAS] ?: 0f, 0.01f)
    }

    @Test
    fun `multiple activation conditions use first matching`() {
        val context = TestContext(battery = 5)
        val battery = context::battery

        val rules = PokemonSpawnDsl(context).pools {
            pool("standard", 100.percent) {
                Pokemon.PIDGEY at 1.0f
            }

            pool("battery_event") {
                activate(50.percent) given { battery.get() < 5 }
                activate(20.percent) given { battery.get() < 10 }
                activate(10.percent) given { battery.get() < 20 }

                Pokemon.VOLTORB at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)

        // Battery at 5 should match second condition (< 10), not third
        val poolProbs = engine.getCurrentPoolProbabilities()
        assertEquals(80f, poolProbs["standard"] ?: 0f, 0.01f)
        assertEquals(20f, poolProbs["battery_event"] ?: 0f, 0.01f)

        // Battery at 2 should match first condition (< 5)
        context.battery = 2
        val engine2 = SpawnRulesEngine(rules)
        val probsLowBattery = engine2.getCurrentPoolProbabilities()
        assertEquals(50f, probsLowBattery["standard"] ?: 0f, 0.01f)
        assertEquals(50f, probsLowBattery["battery_event"] ?: 0f, 0.01f)
    }

    @Test
    fun `scaled activation adjusts event pool percentage dynamically`() {
        val context = TestContext(faceDownMinutes = 90)
        val faceDownMinutes = context::faceDownMinutes

        val rules = PokemonSpawnDsl(context).pools {
            pool("standard", 100.percent) {
                Pokemon.PIDGEY at 1.0f
            }

            pool("legendary") {
                activate(2.percent increaseBy { (faceDownMinutes.get() - 60) / 30.0f * 2.0f }) given
                    { faceDownMinutes.get() > 60 }

                Pokemon.MEW at 1.0f
            }
        }

        val engine = SpawnRulesEngine(rules)

        // At t=90, scale factor is 1 + (90-60)/30 = 2.0
        // So 2% * 2.0 = 4%
        val poolProbs = engine.getCurrentPoolProbabilities()
        assertEquals(96f, poolProbs["standard"] ?: 0f, 0.01f)
        assertEquals(4f, poolProbs["legendary"] ?: 0f, 0.01f)

        // At t=120, scale factor is 1 + (120-60)/30 = 3.0
        // So 2% * 3.0 = 6%
        context.faceDownMinutes = 120
        val engine2 = SpawnRulesEngine(rules)
        val probsLater = engine2.getCurrentPoolProbabilities()
        assertEquals(94f, probsLater["standard"] ?: 0f, 0.01f)
        assertEquals(6f, probsLater["legendary"] ?: 0f, 0.01f)
    }

    @Test
    fun `modifiers can use property reference syntax`() {
        val context = TestContext(night = true)
        val night = context::night

        val rules = PokemonSpawnDsl(context).pools {
            pool("ghosts", 100.percent) {
                Pokemon.GASTLY at 1.0f
                Pokemon.HAUNTER at 1.0f

                // Test property reference syntax for modifiers
                during(night) {
                    boost(Type.GHOST) by 2.0f
                }
            }
        }

        val engine = SpawnRulesEngine(rules)
        val pokemonProbabilities = engine.getPokemonProbabilities("ghosts")

        // Both are ghost type, so both get boosted
        // Weights: 2.0 + 2.0 = 4.0
        assertEquals(50f, pokemonProbabilities[Pokemon.GASTLY] ?: 0f, 0.01f)
        assertEquals(50f, pokemonProbabilities[Pokemon.HAUNTER] ?: 0f, 0.01f)

        // During day, no boost
        context.night = false
        val engine2 = SpawnRulesEngine(rules)
        val probsDay = engine2.getPokemonProbabilities("ghosts")
        assertEquals(50f, probsDay[Pokemon.GASTLY] ?: 0f, 0.01f)
        assertEquals(50f, probsDay[Pokemon.HAUNTER] ?: 0f, 0.01f)
    }

    @Test
    fun `global modifiers apply to all pools`() {
        val context = TestContext(rain = true)
        val rain = context::rain

        val rules = PokemonSpawnDsl(context).pools {
            // Global modifiers that apply everywhere
            modifiers {
                during(rain) {
                    boost(Type.WATER) by 2.0f
                    suppress(Type.FIRE) by 0.5f
                }
            }

            pool("pool1", 50.percent) {
                Pokemon.CHARMANDER at 1.0f // Fire
                Pokemon.SQUIRTLE at 1.0f // Water
            }

            pool("pool2", 50.percent) {
                Pokemon.VULPIX at 1.0f // Fire
                Pokemon.PSYDUCK at 1.0f // Water
            }
        }

        val engine = SpawnRulesEngine(rules)

        // Test pool1
        val pool1Probs = engine.getPokemonProbabilities("pool1")

        // Fire suppressed to 0.5, Water boosted to 2.0
        // Total: 0.5 + 2.0 = 2.5
        assertEquals(20f, pool1Probs[Pokemon.CHARMANDER] ?: 0f, 0.01f) // 0.5/2.5 * 100
        assertEquals(80f, pool1Probs[Pokemon.SQUIRTLE] ?: 0f, 0.01f) // 2.0/2.5 * 100

        // Test pool2 - should have same effect
        val pool2Probs = engine.getPokemonProbabilities("pool2")

        assertEquals(20f, pool2Probs[Pokemon.VULPIX] ?: 0f, 0.01f) // 0.5/2.5 * 100
        assertEquals(80f, pool2Probs[Pokemon.PSYDUCK] ?: 0f, 0.01f) // 2.0/2.5 * 100
    }

    @Test
    fun `global and pool modifiers stack together`() {
        val context = TestContext(rain = true)
        val rain = context::rain

        val rules = PokemonSpawnDsl(context).pools {
            // Global modifier
            modifiers {
                during(rain) {
                    boost(Type.WATER) by 2.0f
                }
            }

            pool("test", 100.percent) {
                Pokemon.SQUIRTLE at 1.0f // Water
                Pokemon.PIDGEY at 1.0f // Normal/Flying

                // Pool-specific modifier that also boosts water
                during(rain) {
                    boost(Type.WATER) by 1.5f
                }
            }
        }

        val engine = SpawnRulesEngine(rules)
        val probs = engine.getPokemonProbabilities("test")

        // Water gets boosted by both global (2.0) and pool (1.5) modifiers
        // Applied multiplicatively: 1.0 * 2.0 * 1.5 = 3.0
        // Total weights: 3.0 + 1.0 = 4.0
        assertEquals(75f, probs[Pokemon.SQUIRTLE] ?: 0f, 0.01f) // 3.0/4.0 * 100
        assertEquals(25f, probs[Pokemon.PIDGEY] ?: 0f, 0.01f) // 1.0/4.0 * 100
    }
}
