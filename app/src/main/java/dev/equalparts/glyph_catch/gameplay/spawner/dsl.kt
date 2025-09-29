@file:Suppress("unused")

package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.gameplay.spawner.models.Modifier
import dev.equalparts.glyph_catch.gameplay.spawner.models.ModifierEffect
import dev.equalparts.glyph_catch.gameplay.spawner.models.PoolActivator
import dev.equalparts.glyph_catch.gameplay.spawner.models.PoolInhabitant
import dev.equalparts.glyph_catch.gameplay.spawner.models.ProbabilityRule
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnPool
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnRules
import dev.equalparts.glyph_catch.gameplay.spawner.models.percent
import kotlin.math.abs
import kotlin.reflect.KProperty0

/**
 * Entry point for the Pokémon spawn rules DSL.
 */
class PokemonSpawnDsl<T>(private val context: T) {

    /**
     * Begins the creation of a [SpawnRules] object.
     */
    fun pools(builder: SpawnRulesBuilder<T>.() -> Unit): SpawnRules = SpawnRulesBuilder(context).apply(builder).build()
}

/**
 * Provides the top-level DSL API.
 */
class SpawnRulesBuilder<T>(internal val context: T) {
    private val pools = mutableListOf<SpawnPool>()
    private val modifiers = mutableListOf<Modifier>()

    /**
     * Define global modifiers that can adjust spawn weights globally based on certain
     * conditions, like the weather.
     */
    fun modifiers(builder: GlobalModifiersBuilder.() -> Unit) {
        val globalBuilder = GlobalModifiersBuilder()
        globalBuilder.apply(builder)
        modifiers.addAll(globalBuilder.build())
    }

    /**
     * Add a base spawn pool.
     *
     * The base spawn pools are always active, although their percentages may be lowered
     * proportionally to make room for dynamic pools (conditional / scaling).
     */
    fun pool(name: String, percentage: Float, builder: SpawnPoolBuilder.() -> Unit) {
        val poolBuilder = SpawnPoolBuilder(name, percentage)
        val pool = poolBuilder.apply(builder).build()
        pools.add(pool)
    }

    /**
     * Add a conditional spawn pool.
     *
     * These pools are only active under specific conditions. When a pool activates with,
     * e.g., a 20% rate, it lends that 20% from the base pools.
     *
     * Use [SpawnPoolBuilder.activate] with `during condition` and `given { condition() }`
     * to specify the activation requirements.
     */
    fun pool(name: String, builder: SpawnPoolBuilder.() -> Unit) {
        val poolBuilder = SpawnPoolBuilder(name, 0.percent)
        poolBuilder.isConditional = true
        val pool = poolBuilder.apply(builder).build()
        pools.add(pool)
    }

    /**
     * Add a dynamic spawn pool with a scaling percentage.
     *
     * These pools are always active, but typically start with a lower percentage that
     * increases based on certain conditions.
     */
    fun pool(name: String, probabilityRule: ProbabilityRule, builder: SpawnPoolBuilder.() -> Unit) {
        val poolBuilder = SpawnPoolBuilder(name, probabilityRule.percentage, probabilityRule.modifier)
        val pool = poolBuilder.apply(builder).build()
        pools.add(pool)
    }

    /**
     * Add an event Pokémon.
     *
     * These function like a conditional pool, but with only one Pokémon in it. The
     * Pokémon will also be displayed as a special discovery in the UI.
     */
    fun special(pokemon: PokemonSpecies, builder: SpawnPoolBuilder.(PokemonSpecies) -> Unit) {
        val poolName = "special:${pokemon.name.lowercase()}"
        val poolBuilder = SpawnPoolBuilder(poolName, 0f)
        poolBuilder.isSpecial = true
        poolBuilder.builder(pokemon)
        poolBuilder.addPokemon(pokemon, 1.0f)
        val pool = poolBuilder.build()
        pools.add(pool)
    }

    /**
     * Validates and builds the spawn rules.
     */
    fun build(): SpawnRules {
        val standardPoolTotal = pools
            .filter { it.basePercentage > 0f }
            .sumOf { it.basePercentage.toDouble() }

        if (abs(standardPoolTotal - 100.0) > 0.01) {
            error("Standard pool percentages must sum to 100%, but got $standardPoolTotal%")
        }

        return SpawnRules(pools, modifiers)
    }
}

/**
 * Provides the DSL API within global modifiers.
 */
class GlobalModifiersBuilder {
    private val modifiers = mutableListOf<Modifier>()

    /**
     * Add one or more global modifiers for a built-in condition.
     */
    fun during(condition: KProperty0<Boolean>, builder: ModifierBuilder.() -> Unit) {
        val modifier = ModifierBuilder { condition.get() }.apply(builder).build()
        modifiers.add(Modifier(modifier.condition, modifier.effects))
    }

    /**
     * Add one or more global modifiers for a custom condition.
     */
    fun during(condition: () -> Boolean, builder: ModifierBuilder.() -> Unit) {
        val modifier = ModifierBuilder(condition).apply(builder).build()
        modifiers.add(Modifier(modifier.condition, modifier.effects))
    }

    fun build(): List<Modifier> = modifiers
}

/**
 * Provides the DSL API within a spawn pool.
 */
open class SpawnPoolBuilder(
    private val name: String,
    private val basePercentage: Float,
    private val baseModifier: ((Float) -> Float)? = null
) {
    protected val entries = mutableListOf<PoolInhabitant>()
    private val activators = mutableListOf<PoolActivator>()
    private val modifiers = mutableListOf<Modifier>()
    internal var isSpecial: Boolean = false
    internal var isConditional: Boolean = false
    private var pendingActivation: ActivationBuilder? = null

    private fun combineConditions(existing: (() -> Boolean)?, new: () -> Boolean): () -> Boolean =
        if (existing != null) {
            { existing() && new() }
        } else {
            new
        }

    /**
     * Define the weight of a Pokémon species in the spawn table.
     *
     * These values can be arbitrary. A typical base weight is 1.0, meaning a
     * Pokémon species with a weight of 2.0 is twice as likely to appear, and
     * a Pokémon species with a weight of 0.5 is half as likely to appear.
     *
     * Example:
     *
     * Given `MAGIKARP at 2.0`, `GOLDEEN at 1.0`, `PSYDUCK at 1.0`, `STARYU at 0.5`,
     * and `SHELLDER at 0.5`: ten spawns from that pool would on average contain
     * four Magikarp, two Goldeen, two Psyduck, one Staryu, and one Shellder.
     *
     * In this example, Magikarp gets a 40% chance of spawning within its pool. If
     * that pool has a base percentage of 20%, that would give Magikarp a 0.2 * 0.4
     * = 8% global spawn chance.
     */
    infix fun PokemonSpecies.at(weight: Float): PokemonEntryBuilder = PokemonEntryBuilder(this, weight)

    internal fun addPokemon(species: PokemonSpecies, weight: Float) {
        entries.add(PoolInhabitant(species, weight))
    }

    /**
     * Provides the DSL API within a Pokémon species rule.
     */
    inner class PokemonEntryBuilder(private val species: PokemonSpecies, private val weight: Float) {
        init {
            entries.add(PoolInhabitant(species, weight))
        }

        /**
         * Mark this Pokémon as a conditional spawn based on a specific condition, e.g.,
         * a Ghost type that only appears at night.
         *
         * Set a custom condition for this Pokémon species to appear.
         */
        infix fun given(condition: () -> Boolean): PokemonEntryBuilder {
            val lastEntry = entries.removeAt(entries.size - 1)
            val combinedCondition = combineConditions(lastEntry.condition, condition)
            entries.add(PoolInhabitant(species, weight, combinedCondition))
            return this
        }

        /**
         * Set a standard condition for this Pokémon species to appear.
         */
        infix fun during(condition: KProperty0<Boolean>): PokemonEntryBuilder {
            val lastEntry = entries.removeAt(entries.size - 1)
            val combinedCondition = combineConditions(lastEntry.condition) { condition.get() }
            entries.add(PoolInhabitant(species, weight, combinedCondition))
            return this
        }
    }

    /**
     * Add a condition for this pool to activate with the given percentage.
     *
     * Use with `during condition` and `given { condition() }` to specify the activation
     * requirements. If multiple requirements are specified with varying percentages,
     * the first match is applied.
     */
    fun activate(percentage: Float): ActivationBuilder {
        pendingActivation?.build()
        return ActivationBuilder(percentage)
    }

    /**
     * Add a condition for this pool to activate with the given dynamic percentage.
     *
     * Use with `during condition` and `given { condition() }` to specify the activation
     * requirements. If multiple requirements are specified with varying percentages,
     * the first match is applied.
     */
    fun activate(probabilityRule: ProbabilityRule): ActivationBuilder {
        pendingActivation?.build()
        return ActivationBuilder(probabilityRule.percentage, probabilityRule.modifier)
    }

    /**
     * Provides the DSL API within an [activate] rule.
     */
    inner class ActivationBuilder(private val percentage: Float, private val modifier: ((Float) -> Float)? = null) {
        private var currentCondition: (() -> Boolean)? = null

        init {
            pendingActivation = this
        }

        /**
         * Specify a standard condition for activating the pool, e.g., `during halloween`.
         */
        infix fun during(condition: KProperty0<Boolean>): ActivationBuilder {
            currentCondition = combineConditions(currentCondition) { condition.get() }
            return this
        }

        /**
         * Specify a custom condition for activating the pool, e.g., `given { something() < 5.0 }`.
         */
        infix fun given(condition: () -> Boolean): ActivationBuilder {
            currentCondition = combineConditions(currentCondition, condition)
            return this
        }

        internal fun build() {
            if (currentCondition == null) {
                error("No activation condition specified")
            }
            activators.add(PoolActivator(percentage, currentCondition!!, modifier))
            pendingActivation = null
        }
    }

    /**
     * Add one or more global modifiers for a built-in condition.
     */
    fun during(condition: KProperty0<Boolean>, builder: ModifierBuilder.() -> Unit) {
        val modifier = ModifierBuilder { condition.get() }.apply(builder).build()
        modifiers.add(modifier)
    }

    /**
     * Add one or more global modifiers for a custom condition.
     */
    fun during(condition: () -> Boolean, builder: ModifierBuilder.() -> Unit) {
        val modifier = ModifierBuilder(condition).apply(builder).build()
        modifiers.add(modifier)
    }

    fun build(): SpawnPool {
        pendingActivation?.build()
        return SpawnPool(name, basePercentage, entries, activators, modifiers, baseModifier, isSpecial, isConditional)
    }
}

/**
 * The DSL API within conditional spawn modifiers.
 */
class ModifierBuilder(private val condition: () -> Boolean) {
    private val effects = mutableListOf<ModifierEffect>()

    /**
     * Increase the likelihood of this Pokémon type to appear.
     */
    fun boost(type: Type) = TypeModifierBuilder(type, true)

    /**
     * Decrease the likelihood of this Pokémon type to appear.
     */
    fun suppress(type: Type) = TypeModifierBuilder(type, false)

    inner class TypeModifierBuilder(private val type: Type, private val isBoost: Boolean) {
        infix fun by(multiplier: Float) {
            val effect = if (isBoost) {
                ModifierEffect.BoostType(type, multiplier)
            } else {
                ModifierEffect.SuppressType(type, multiplier)
            }
            effects.add(effect)
        }
    }

    /**
     * Add a Pokémon to the spawn pool.
     */
    fun add(pokemon: PokemonSpecies) = AddPokemonBuilder(pokemon)

    inner class AddPokemonBuilder(private val pokemon: PokemonSpecies) {
        infix fun at(weight: Float) {
            effects.add(ModifierEffect.AddPokemon(pokemon, weight))
        }
    }

    fun build() = Modifier(condition, effects)
}
