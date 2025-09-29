package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnRules
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnScaling
import dev.equalparts.glyph_catch.gameplay.spawner.models.increaseBy
import dev.equalparts.glyph_catch.gameplay.spawner.models.percent
import kotlin.time.Duration.Companion.minutes

/**
 * This defines the spawn rules for the game using a custom DSL.
 */
fun createSpawnRules(context: GameplayContext): SpawnRules {
    val dsl = PokemonSpawnDsl(context)

    val time = context.time
    val weather = context.weather
    val season = context.season
    val events = context.events
    val phone = context.phone
    val trainer = context.trainer
    val scaling = SpawnScaling(context)

    return dsl.pools {
        // Global modifiers
        // ================
        //
        // These affect all spawn pools, boosting certain types and suppressing
        // others based on weather conditions and time of day.

        modifiers {
            during(weather::rain) {
                boost(Type.WATER) by 5.0f
                boost(Type.GRASS) by 3.0f
            }

            during(weather::thunderstorm) {
                boost(Type.ELECTRIC) by 5.0f
                boost(Type.STEEL) by 3.0f
            }

            during(weather::snow) {
                boost(Type.ICE) by 5.0f
                suppress(Type.GRASS) by 0.5f
                suppress(Type.FIRE) by 0.5f
            }

            during(time::night) {
                suppress(Type.NORMAL) by 0.5f
            }
        }

        // Starter events
        // ==============
        //
        // The player can catch one of the three starter Pokémon shortly after
        // starting the game. The other two starters will be available from
        // the rare spawn pool.

        val starters = listOf(
            Pokemon.BULBASAUR,
            Pokemon.CHARMANDER,
            Pokemon.SQUIRTLE
        )

        starters.forEach {
            special(it) {
                activate(33.percent) given { trainer.hasNotFoundAny(starters) }
            }
        }

        // Common Pokémon
        // ==============
        //
        // As long as the player keeps their phone screen off, this percentage will
        // slowly be reallocated to other groups.

        pool("Common", 87.percent) {
            Pokemon.CATERPIE at 2.0f
            Pokemon.WEEDLE at 2.0f
            Pokemon.PIDGEY at 2.0f
            Pokemon.RATTATA at 2.0f
            Pokemon.SPEAROW at 1.0f
            Pokemon.EKANS at 1.0f
            Pokemon.SANDSHREW at 1.0f
            Pokemon.NIDORAN_F at 1.0f
            Pokemon.NIDORAN_M at 1.0f
            Pokemon.ODDISH at 1.0f
            Pokemon.PARAS at 1.0f
            Pokemon.DIGLETT at 1.0f
            Pokemon.MEOWTH at 1.0f
            Pokemon.PSYDUCK at 1.0f
            Pokemon.MANKEY at 1.0f
            Pokemon.POLIWAG at 1.0f
            Pokemon.MACHOP at 1.0f
            Pokemon.BELLSPROUT at 1.0f
            Pokemon.GEODUDE at 1.0f
            Pokemon.DODUO at 1.0f
            Pokemon.GRIMER at 1.0f
            Pokemon.KOFFING at 1.0f
            Pokemon.DROWZEE at 1.0f
            Pokemon.KRABBY at 1.0f
            Pokemon.GASTLY at 0.3f during time::night
            Pokemon.VENONAT at 3.0f during time::night
            Pokemon.ZUBAT at 4.0f during time::night
        }

        // Fishing pool
        // ============
        //
        // Water-bound Pokémon that require the Super Rod to catch. These Pokémon don't
        // really make sense on land, so they only appear when fishing.

        pool("Fishing") {
            activate(70.percent) given { trainer.isUsingItem(Item.SUPER_ROD) }

            Pokemon.MAGIKARP at 3.0f
            Pokemon.TENTACOOL at 2.0f
            Pokemon.GOLDEEN at 1.5f
            Pokemon.SHELLDER at 1.5f
            Pokemon.HORSEA at 1.0f
            Pokemon.STARYU at 1.0f
        }

        // Uncommon & Rare
        // ===============
        //
        // These are rarer spawns with time-based scaling rules. The longer the
        // phone screen stays turned off, the higher the percentage gets.

        pool(
            "Uncommon",
            10.percent increaseBy {
                scaling.timeBoost(
                    gain = 30.percent,
                    over = 120.minutes
                ) + scaling.sleepBonus(5.percent)
            }
        ) {
            Pokemon.PIKACHU at 1.0f
            Pokemon.VULPIX at 1.0f
            Pokemon.JIGGLYPUFF at 1.0f
            Pokemon.PONYTA at 1.0f
            Pokemon.SLOWPOKE at 1.0f
            Pokemon.FARFETCHD at 1.0f
            Pokemon.SEEL at 1.0f
            Pokemon.ONIX at 1.0f
            Pokemon.EXEGGCUTE at 1.0f
            Pokemon.TANGELA at 1.0f
            Pokemon.GROWLITHE at 1.0f
            Pokemon.CUBONE at 1.0f
            Pokemon.LICKITUNG at 1.0f
            Pokemon.RHYHORN at 1.0f
            Pokemon.KANGASKHAN at 1.0f
            Pokemon.CHANSEY at 0.5f
            Pokemon.MR_MIME at 0.5f
            Pokemon.SCYTHER at 0.5f
            Pokemon.JYNX at 0.5f
            Pokemon.ELECTABUZZ at 0.5f
            Pokemon.MAGMAR at 0.5f
            Pokemon.PINSIR at 0.5f
            Pokemon.TAUROS at 1.0f
            Pokemon.EEVEE at 1.0f
        }

        pool(
            "Rare",
            3.percent increaseBy {
                scaling.timeBoost(
                    gain = 5.percent,
                    over = 180.minutes
                ) + scaling.sleepBonus(5.percent)
            }
        ) {
            Pokemon.BULBASAUR at 1f
            Pokemon.CHARMANDER at 1f
            Pokemon.SQUIRTLE at 1f

            Pokemon.ABRA at 1.0f
            Pokemon.DITTO at 1.0f
            Pokemon.PORYGON at 1.0f
            Pokemon.DRATINI at 0.5f

            Pokemon.LAPRAS at 3.0f during weather::rain
        }

        // Fossil events
        // =============
        //
        // As the player progresses their Pokédex, fossil Pokémon appear at
        // various milestones, with either Omanyte or Kabuto appearing first.

        val fossils = listOf(
            Pokemon.OMANYTE, // ༼ つ ◕_◕ ༽つ
            Pokemon.KABUTO
        )

        fossils.forEach { fossil ->
            special(fossil) {
                activate(30.percent) given { trainer.hasNotFoundAny(fossils) && trainer.pokedexCount >= 30 }
                activate(30.percent) given { trainer.hasNotFound(it) && trainer.pokedexCount >= 60 }
            }
        }

        special(Pokemon.AERODACTYL) {
            activate(30.percent) given { trainer.hasNotFound(it) && trainer.pokedexCount >= 90 }
        }

        // Halloween event
        // ===============
        //
        // The Gastly and Zubat lines appear all day during the spooky season,
        // including evolved forms at night.

        pool("Halloween") {
            activate(30.percent) during events::halloween during time::night
            activate(15.percent) during events::halloween

            Pokemon.GASTLY at 1.0f
            Pokemon.ZUBAT at 1.0f

            Pokemon.HAUNTER at 2.0f during time::night
            Pokemon.GENGAR at 1.0f during time::night
            Pokemon.GOLBAT at 2.0f during time::night
        }

        // Christmas event
        // ===============
        //
        // During the holiday season, Ice-type Pokémon will appear more commonly.
        // Additionally, the Johto Pokémon Stantler and Delibird can each be
        // caught once.

        pool("Christmas") {
            activate(10.percent) during events::christmas

            Pokemon.JYNX at 2.0f
            Pokemon.SEEL at 1.5f
            Pokemon.DEWGONG at 1.0f
            Pokemon.LAPRAS at 0.25f
        }

        special(Pokemon.DELIBIRD) {
            activate(10.percent) during events::christmas given { trainer.hasNotFound(it) }
        }

        special(Pokemon.STANTLER) {
            activate(10.percent) during events::christmas given { trainer.hasNotFound(it) }
        }

        // Thunderstorm event
        // ==================
        //
        // During thunderstorms, certain Electric-type Pokémon will frequently
        // appear, including evolved forms.

        pool("Thunderstorm") {
            activate(30.percent) during weather::thunderstorm

            Pokemon.PIKACHU at 3.0f
            Pokemon.RAICHU at 1.0f
            Pokemon.MAGNEMITE at 2.0f
            Pokemon.MAGNETON at 1.0f
        }

        // Low battery event
        // =================
        //
        // When the phone battery runs low, certain Electric-type Pokémon will
        // appear, including a guaranteed Electrode spawn below 10% battery.

        special(Pokemon.VOLTORB) {
            activate(20.percent) given { phone.battery < 15 }
        }

        special(Pokemon.ELECTRODE) {
            activate(100.percent) given { trainer.hasNotFound(it) && phone.battery < 10 }
        }

        // Fighters
        // =======
        //
        // Hitmonchan and Hitmonlee spawn based on training dedication (consecutive days
        // with a training partner).

        special(Pokemon.HITMONCHAN) {
            activate(100.percent) given { trainer.currentPartnerDays >= 7 && trainer.hasNotFound(it) }
        }

        special(Pokemon.HITMONLEE) {
            activate(100.percent) given {
                trainer.currentPartnerDays >= 7 &&
                    trainer.daysSinceLastFighterCaught >= 7 &&
                    trainer.hasNotFound(it)
            }
        }

        // Full moon event
        // ===============
        //
        // During the monthly full moon, various Fairy-type Pokémon will be common
        // spawns during both day and night.

        pool("Full moon") {
            activate(30.percent) during events::fullMoon

            Pokemon.CLEFAIRY at 5.0f during time::night
            Pokemon.CLEFABLE at 1.0f during time::night
            Pokemon.JIGGLYPUFF at 2.0f during time::day
            Pokemon.WIGGLYTUFF at 2.0f during time::day
        }

        // Snorlax event
        // =============
        //
        // Snorlax appears after N hours of screen off time. A one-time guaranteed
        // spawn occurs after 8 hours of sleep.

        special(Pokemon.SNORLAX) {
            val threshold = 8 * 60
            activate(100.percent) given { trainer.hasNotFound(it) && phone.minutesOff > threshold }
            activate(1.percent) given { phone.minutesOff > threshold }
        }

        // Legendary bird events
        // =====================
        //
        // The legendary birds are one-time spawns with special conditions:
        //
        // - Zapdos spawns during weather::thunderstorms after the player has caught
        //   a number of other Pokémon already. For areas that get few or no
        //   thunderstorms, there will be a fallback item.
        //
        // - Articuno and Moltres have a tiny chance of appearing during season::winter
        //   and summer respectively, after the phone screen has been off for
        //   at least one hour. Chances increase with screen off time.

        special(Pokemon.ZAPDOS) {
            activate(50.percent) during weather::thunderstorm given {
                trainer.hasNotFound(it) && trainer.pokedexCount > 30
            }
        }

        special(Pokemon.ARTICUNO) {
            activate(1.percent increaseBy { scaling.minutes / 60f }) during season::winter given {
                trainer.hasNotFound(it) && scaling.minutes > 30
            }
        }

        special(Pokemon.MOLTRES) {
            activate(1.percent increaseBy { scaling.minutes / 60f }) during season::summer given {
                trainer.hasNotFound(it) && scaling.minutes > 30
            }
        }

        // Mewtwo event
        // ============
        //
        // TODO: Come up with a mechanic. Probably N Pokémon leveled to lv70+?

        special(Pokemon.MEWTWO) {
            activate(1.percent) given { false }
        }

        // Mew event
        // =========
        //
        // Mew is a one-time spawn when all other 150 Pokémon have been caught.

        special(Pokemon.MEW) {
            activate(10.percent) given { trainer.hasNotFound(it) && trainer.pokedexCount >= 150 }
        }
    }
}
