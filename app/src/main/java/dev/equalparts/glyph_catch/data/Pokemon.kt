package dev.equalparts.glyph_catch.data

/**
 * Pokémon species definition for storage in the [Pokemon] object.
 */
data class PokemonSpecies(
    val id: Int,
    val name: String,
    val type1: Type,
    val type2: Type? = null,
    val evolvesTo: MutableList<Int> = mutableListOf(),
    val evolutionRequirement: EvolutionRequirement? = null,
    val genderRatio: Double,
    val eggGroups: List<EggGroup>
)

/**
 * Pokémon type identifiers.
 */
@Suppress("unused")
enum class Type {
    NORMAL, FIRE, WATER, ELECTRIC, GRASS, ICE, FIGHTING, POISON, GROUND,
    FLYING, PSYCHIC, BUG, ROCK, GHOST, DRAGON, DARK, STEEL, FAIRY
}

/**
 * Pokémon egg groups.
 */
enum class EggGroup {
    MONSTER, WATER1, BUG, FLYING, FIELD, FAIRY, GRASS, HUMAN_LIKE,
    WATER3, MINERAL, AMORPHOUS, WATER2, DITTO, DRAGON, NO_EGGS
}

enum class TimeOfDay {
    DAY, NIGHT
}

/**
 * Evolution requirement definition for a [PokemonSpecies].
 */
sealed class EvolutionRequirement {
    data class Level(val level: Int) : EvolutionRequirement()
    data class Stone(val item: Item) : EvolutionRequirement()
    object Happiness : EvolutionRequirement()
    data class TimeHappiness(val time: TimeOfDay) : EvolutionRequirement()
    object Trade : EvolutionRequirement()
}

/**
 * Obtainable item identifiers.
 */
enum class Item {
    FIRE_STONE,
    WATER_STONE,
    THUNDER_STONE,
    LEAF_STONE,
    MOON_STONE,
    SUN_STONE,
    SUPER_ROD,
    RARE_CANDY,
    LINKING_CORD,
    REPEL,
    SOOTHE_BELL_COOKIE
}

/**
 * The list of Pokémon species that have been added to the game.
 */
@Suppress("unused")
object Pokemon {
    private val entries = mutableMapOf<Int, PokemonSpecies>()

    data class EvolutionFrom(val source: PokemonSpecies, val requirement: EvolutionRequirement)

    infix fun PokemonSpecies.at(level: Int) = EvolutionFrom(this, EvolutionRequirement.Level(level))
    infix fun PokemonSpecies.with(stone: Item) = EvolutionFrom(this, EvolutionRequirement.Stone(stone))
    val PokemonSpecies.byHappiness get() = EvolutionFrom(this, EvolutionRequirement.Happiness)
    val PokemonSpecies.byDayHappiness get() = EvolutionFrom(this, EvolutionRequirement.TimeHappiness(TimeOfDay.DAY))
    val PokemonSpecies.byNightHappiness get() = EvolutionFrom(this, EvolutionRequirement.TimeHappiness(TimeOfDay.NIGHT))
    val PokemonSpecies.byTrade get() = EvolutionFrom(this, EvolutionRequirement.Trade)

    private fun add(
        id: Int,
        name: String,
        type1: Type,
        type2: Type? = null,
        genderRatio: Double = -1.0,
        eggGroups: List<EggGroup> = emptyList(),
        from: EvolutionFrom? = null
    ): PokemonSpecies {
        val species = PokemonSpecies(
            id, name, type1, type2,
            genderRatio = genderRatio,
            eggGroups = eggGroups,
            evolutionRequirement = from?.requirement
        )
        entries[id] = species
        from?.source?.evolvesTo?.add(id)
        return species
    }
    val BULBASAUR = add(1, "Bulbasaur", Type.GRASS, Type.POISON, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.GRASS))
    val IVYSAUR = add(2, "Ivysaur", Type.GRASS, Type.POISON, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.GRASS), from = BULBASAUR at 16)
    val VENUSAUR = add(3, "Venusaur", Type.GRASS, Type.POISON, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.GRASS), from = IVYSAUR at 32)
    val CHARMANDER = add(4, "Charmander", Type.FIRE, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.DRAGON))
    val CHARMELEON = add(5, "Charmeleon", Type.FIRE, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.DRAGON), from = CHARMANDER at 16)
    val CHARIZARD = add(6, "Charizard", Type.FIRE, Type.FLYING, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.DRAGON), from = CHARMELEON at 36)
    val SQUIRTLE = add(7, "Squirtle", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1))
    val WARTORTLE = add(8, "Wartortle", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1), from = SQUIRTLE at 16)
    val BLASTOISE = add(9, "Blastoise", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1), from = WARTORTLE at 36)
    val CATERPIE = add(10, "Caterpie", Type.BUG, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val METAPOD = add(11, "Metapod", Type.BUG, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = CATERPIE at 7)
    val BUTTERFREE = add(12, "Butterfree", Type.BUG, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = METAPOD at 10)
    val WEEDLE = add(13, "Weedle", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val KAKUNA = add(14, "Kakuna", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = WEEDLE at 7)
    val BEEDRILL = add(15, "Beedrill", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = KAKUNA at 10)
    val PIDGEY = add(16, "Pidgey", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val PIDGEOTTO = add(17, "Pidgeotto", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = PIDGEY at 18)
    val PIDGEOT = add(18, "Pidgeot", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = PIDGEOTTO at 36)
    val RATTATA = add(19, "Rattata", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val RATICATE = add(20, "Raticate", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = RATTATA at 20)
    val SPEAROW = add(21, "Spearow", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val FEAROW = add(22, "Fearow", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = SPEAROW at 20)
    val EKANS = add(23, "Ekans", Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD, EggGroup.DRAGON))
    val ARBOK = add(24, "Arbok", Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD, EggGroup.DRAGON), from = EKANS at 22)
    val PICHU = add(172, "Pichu", Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.NO_EGGS))
    val PIKACHU = add(25, "Pikachu", Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD, EggGroup.FAIRY), from = PICHU.byHappiness)
    val RAICHU = add(26, "Raichu", Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD, EggGroup.FAIRY), from = PIKACHU with Item.THUNDER_STONE)
    val SANDSHREW = add(27, "Sandshrew", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val SANDSLASH = add(28, "Sandslash", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = SANDSHREW at 22)
    val NIDORAN_F = add(29, "Nidoran♀", Type.POISON, genderRatio = 1.0, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD))
    val NIDORINA = add(30, "Nidorina", Type.POISON, genderRatio = 1.0, eggGroups = listOf(EggGroup.NO_EGGS), from = NIDORAN_F at 16)
    val NIDOQUEEN = add(31, "Nidoqueen", Type.POISON, Type.GROUND, genderRatio = 1.0, eggGroups = listOf(EggGroup.NO_EGGS), from = NIDORINA with Item.MOON_STONE)
    val NIDORAN_M = add(32, "Nidoran♂", Type.POISON, genderRatio = 0.0, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD))
    val NIDORINO = add(33, "Nidorino", Type.POISON, genderRatio = 0.0, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD), from = NIDORAN_M at 16)
    val NIDOKING = add(34, "Nidoking", Type.POISON, Type.GROUND, genderRatio = 0.0, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD), from = NIDORINO with Item.MOON_STONE)
    val CLEFFA = add(173, "Cleffa", Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.NO_EGGS))
    val CLEFAIRY = add(35, "Clefairy", Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.FAIRY), from = CLEFFA.byHappiness)
    val CLEFABLE = add(36, "Clefable", Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.FAIRY), from = CLEFAIRY with Item.MOON_STONE)
    val VULPIX = add(37, "Vulpix", Type.FIRE, genderRatio = 0.75, eggGroups = listOf(EggGroup.FIELD))
    val NINETALES = add(38, "Ninetales", Type.FIRE, genderRatio = 0.75, eggGroups = listOf(EggGroup.FIELD), from = VULPIX with Item.FIRE_STONE)
    val IGGLYBUFF = add(174, "Igglybuff", Type.NORMAL, Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.NO_EGGS))
    val JIGGLYPUFF = add(39, "Jigglypuff", Type.NORMAL, Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.FAIRY), from = IGGLYBUFF.byHappiness)
    val WIGGLYTUFF = add(40, "Wigglytuff", Type.NORMAL, Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.FAIRY), from = JIGGLYPUFF with Item.MOON_STONE)
    val ZUBAT = add(41, "Zubat", Type.POISON, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val GOLBAT = add(42, "Golbat", Type.POISON, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = ZUBAT at 22)
    val ODDISH = add(43, "Oddish", Type.GRASS, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS))
    val GLOOM = add(44, "Gloom", Type.GRASS, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = ODDISH at 21)
    val VILEPLUME = add(45, "Vileplume", Type.GRASS, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = GLOOM with Item.LEAF_STONE)
    val PARAS = add(46, "Paras", Type.BUG, Type.GRASS, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG, EggGroup.GRASS))
    val PARASECT = add(47, "Parasect", Type.BUG, Type.GRASS, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG, EggGroup.GRASS), from = PARAS at 24)
    val VENONAT = add(48, "Venonat", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val VENOMOTH = add(49, "Venomoth", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = VENONAT at 31)
    val DIGLETT = add(50, "Diglett", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val DUGTRIO = add(51, "Dugtrio", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = DIGLETT at 26)
    val MEOWTH = add(52, "Meowth", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val PERSIAN = add(53, "Persian", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = MEOWTH at 28)
    val PSYDUCK = add(54, "Psyduck", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD))
    val GOLDUCK = add(55, "Golduck", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD), from = PSYDUCK at 33)
    val MANKEY = add(56, "Mankey", Type.FIGHTING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val PRIMEAPE = add(57, "Primeape", Type.FIGHTING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = MANKEY at 28)
    val GROWLITHE = add(58, "Growlithe", Type.FIRE, genderRatio = 0.25, eggGroups = listOf(EggGroup.FIELD))
    val ARCANINE = add(59, "Arcanine", Type.FIRE, genderRatio = 0.25, eggGroups = listOf(EggGroup.FIELD), from = GROWLITHE with Item.FIRE_STONE)
    val POLIWAG = add(60, "Poliwag", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1))
    val POLIWHIRL = add(61, "Poliwhirl", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1), from = POLIWAG at 25)
    val POLIWRATH = add(62, "Poliwrath", Type.WATER, Type.FIGHTING, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1), from = POLIWHIRL with Item.WATER_STONE)
    val ABRA = add(63, "Abra", Type.PSYCHIC, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE))
    val KADABRA = add(64, "Kadabra", Type.PSYCHIC, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = ABRA at 16)
    val ALAKAZAM = add(65, "Alakazam", Type.PSYCHIC, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = KADABRA.byTrade)
    val MACHOP = add(66, "Machop", Type.FIGHTING, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE))
    val MACHOKE = add(67, "Machoke", Type.FIGHTING, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = MACHOP at 28)
    val MACHAMP = add(68, "Machamp", Type.FIGHTING, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = MACHOKE.byTrade)
    val BELLSPROUT = add(69, "Bellsprout", Type.GRASS, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS))
    val WEEPINBELL = add(70, "Weepinbell", Type.GRASS, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = BELLSPROUT at 21)
    val VICTREEBEL = add(71, "Victreebel", Type.GRASS, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = WEEPINBELL with Item.LEAF_STONE)
    val TENTACOOL = add(72, "Tentacool", Type.WATER, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER3))
    val TENTACRUEL = add(73, "Tentacruel", Type.WATER, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER3), from = TENTACOOL at 30)
    val GEODUDE = add(74, "Geodude", Type.ROCK, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MINERAL))
    val GRAVELER = add(75, "Graveler", Type.ROCK, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MINERAL), from = GEODUDE at 25)
    val GOLEM = add(76, "Golem", Type.ROCK, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MINERAL), from = GRAVELER.byTrade)
    val PONYTA = add(77, "Ponyta", Type.FIRE, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val RAPIDASH = add(78, "Rapidash", Type.FIRE, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = PONYTA at 40)
    val SLOWPOKE = add(79, "Slowpoke", Type.WATER, Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1))
    val SLOWBRO = add(80, "Slowbro", Type.WATER, Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1), from = SLOWPOKE at 37)
    val MAGNEMITE = add(81, "Magnemite", Type.ELECTRIC, Type.STEEL, genderRatio = -1.0, eggGroups = listOf(EggGroup.MINERAL))
    val MAGNETON = add(82, "Magneton", Type.ELECTRIC, Type.STEEL, genderRatio = -1.0, eggGroups = listOf(EggGroup.MINERAL), from = MAGNEMITE at 30)
    val FARFETCHD = add(83, "Farfetch'd", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING, EggGroup.FIELD))
    val DODUO = add(84, "Doduo", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val DODRIO = add(85, "Dodrio", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = DODUO at 31)
    val SEEL = add(86, "Seel", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD))
    val DEWGONG = add(87, "Dewgong", Type.WATER, Type.ICE, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD), from = SEEL at 34)
    val GRIMER = add(88, "Grimer", Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS))
    val MUK = add(89, "Muk", Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS), from = GRIMER at 38)
    val SHELLDER = add(90, "Shellder", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER3))
    val CLOYSTER = add(91, "Cloyster", Type.WATER, Type.ICE, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER3), from = SHELLDER with Item.WATER_STONE)
    val GASTLY = add(92, "Gastly", Type.GHOST, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS))
    val HAUNTER = add(93, "Haunter", Type.GHOST, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS), from = GASTLY at 25)
    val GENGAR = add(94, "Gengar", Type.GHOST, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS), from = HAUNTER.byTrade)
    val ONIX = add(95, "Onix", Type.ROCK, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MINERAL))
    val DROWZEE = add(96, "Drowzee", Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.HUMAN_LIKE))
    val HYPNO = add(97, "Hypno", Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = DROWZEE at 26)
    val KRABBY = add(98, "Krabby", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER3))
    val KINGLER = add(99, "Kingler", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER3), from = KRABBY at 28)
    val VOLTORB = add(100, "Voltorb", Type.ELECTRIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.MINERAL))
    val ELECTRODE = add(101, "Electrode", Type.ELECTRIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.MINERAL), from = VOLTORB at 30)
    val EXEGGCUTE = add(102, "Exeggcute", Type.GRASS, Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS))
    val EXEGGUTOR = add(103, "Exeggutor", Type.GRASS, Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = EXEGGCUTE with Item.LEAF_STONE)
    val CUBONE = add(104, "Cubone", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER))
    val MAROWAK = add(105, "Marowak", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER), from = CUBONE at 28)
    val TYROGUE = add(236, "Tyrogue", Type.FIGHTING, genderRatio = 0.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val HITMONLEE = add(106, "Hitmonlee", Type.FIGHTING, genderRatio = 0.0, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = TYROGUE at 20)
    val HITMONCHAN = add(107, "Hitmonchan", Type.FIGHTING, genderRatio = 0.0, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = TYROGUE at 20)
    val LICKITUNG = add(108, "Lickitung", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER))
    val KOFFING = add(109, "Koffing", Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS))
    val WEEZING = add(110, "Weezing", Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS), from = KOFFING at 35)
    val RHYHORN = add(111, "Rhyhorn", Type.GROUND, Type.ROCK, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD))
    val RHYDON = add(112, "Rhydon", Type.GROUND, Type.ROCK, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD), from = RHYHORN at 42)
    val CHANSEY = add(113, "Chansey", Type.NORMAL, genderRatio = 1.0, eggGroups = listOf(EggGroup.FAIRY))
    val TANGELA = add(114, "Tangela", Type.GRASS, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS))
    val KANGASKHAN = add(115, "Kangaskhan", Type.NORMAL, genderRatio = 1.0, eggGroups = listOf(EggGroup.MONSTER))
    val HORSEA = add(116, "Horsea", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.DRAGON))
    val SEADRA = add(117, "Seadra", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.DRAGON), from = HORSEA at 32)
    val GOLDEEN = add(118, "Goldeen", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2))
    val SEAKING = add(119, "Seaking", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2), from = GOLDEEN at 33)
    val STARYU = add(120, "Staryu", Type.WATER, genderRatio = -1.0, eggGroups = listOf(EggGroup.WATER3))
    val STARMIE = add(121, "Starmie", Type.WATER, Type.PSYCHIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.WATER3), from = STARYU with Item.WATER_STONE)
    val MR_MIME = add(122, "Mr. Mime", Type.PSYCHIC, Type.FAIRY, genderRatio = 0.5, eggGroups = listOf(EggGroup.HUMAN_LIKE))
    val SCYTHER = add(123, "Scyther", Type.BUG, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val SMOOCHUM = add(238, "Smoochum", Type.ICE, Type.PSYCHIC, genderRatio = 1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val JYNX = add(124, "Jynx", Type.ICE, Type.PSYCHIC, genderRatio = 1.0, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = SMOOCHUM at 30)
    val ELEKID = add(239, "Elekid", Type.ELECTRIC, genderRatio = 0.25, eggGroups = listOf(EggGroup.NO_EGGS))
    val ELECTABUZZ = add(125, "Electabuzz", Type.ELECTRIC, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = ELEKID at 30)
    val MAGBY = add(240, "Magby", Type.FIRE, genderRatio = 0.25, eggGroups = listOf(EggGroup.NO_EGGS))
    val MAGMAR = add(126, "Magmar", Type.FIRE, genderRatio = 0.25, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = MAGBY at 30)
    val PINSIR = add(127, "Pinsir", Type.BUG, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val TAUROS = add(128, "Tauros", Type.NORMAL, genderRatio = 0.0, eggGroups = listOf(EggGroup.FIELD))
    val MAGIKARP = add(129, "Magikarp", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2, EggGroup.DRAGON))
    val GYARADOS = add(130, "Gyarados", Type.WATER, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2, EggGroup.DRAGON), from = MAGIKARP at 20)
    val LAPRAS = add(131, "Lapras", Type.WATER, Type.ICE, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1))
    val DITTO = add(132, "Ditto", Type.NORMAL, genderRatio = -1.0, eggGroups = listOf(EggGroup.DITTO))
    val EEVEE = add(133, "Eevee", Type.NORMAL, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD))
    val VAPOREON = add(134, "Vaporeon", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = EEVEE with Item.WATER_STONE)
    val JOLTEON = add(135, "Jolteon", Type.ELECTRIC, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = EEVEE with Item.THUNDER_STONE)
    val FLAREON = add(136, "Flareon", Type.FIRE, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = EEVEE with Item.FIRE_STONE)
    val PORYGON = add(137, "Porygon", Type.NORMAL, genderRatio = -1.0, eggGroups = listOf(EggGroup.MINERAL))
    val OMANYTE = add(138, "Omanyte", Type.ROCK, Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER3))
    val OMASTAR = add(139, "Omastar", Type.ROCK, Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER3), from = OMANYTE at 40)
    val KABUTO = add(140, "Kabuto", Type.ROCK, Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER3))
    val KABUTOPS = add(141, "Kabutops", Type.ROCK, Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER3), from = KABUTO at 40)
    val AERODACTYL = add(142, "Aerodactyl", Type.ROCK, Type.FLYING, genderRatio = 0.125, eggGroups = listOf(EggGroup.FLYING))
    val SNORLAX = add(143, "Snorlax", Type.NORMAL, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER))
    val ARTICUNO = add(144, "Articuno", Type.ICE, Type.FLYING, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val ZAPDOS = add(145, "Zapdos", Type.ELECTRIC, Type.FLYING, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val MOLTRES = add(146, "Moltres", Type.FIRE, Type.FLYING, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val DRATINI = add(147, "Dratini", Type.DRAGON, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.DRAGON))
    val DRAGONAIR = add(148, "Dragonair", Type.DRAGON, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.DRAGON), from = DRATINI at 30)
    val DRAGONITE = add(149, "Dragonite", Type.DRAGON, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.DRAGON), from = DRAGONAIR at 55)
    val MEWTWO = add(150, "Mewtwo", Type.PSYCHIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val MEW = add(151, "Mew", Type.PSYCHIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val CHIKORITA = add(152, "Chikorita", Type.GRASS, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.GRASS))
    val BAYLEEF = add(153, "Bayleef", Type.GRASS, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.GRASS), from = CHIKORITA at 16)
    val MEGANIUM = add(154, "Meganium", Type.GRASS, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.GRASS), from = BAYLEEF at 32)
    val CYNDAQUIL = add(155, "Cyndaquil", Type.FIRE, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD))
    val QUILAVA = add(156, "Quilava", Type.FIRE, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = CYNDAQUIL at 14)
    val TYPHLOSION = add(157, "Typhlosion", Type.FIRE, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = QUILAVA at 36)
    val TOTODILE = add(158, "Totodile", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1))
    val CROCONAW = add(159, "Croconaw", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1), from = TOTODILE at 18)
    val FERALIGATR = add(160, "Feraligatr", Type.WATER, genderRatio = 0.125, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1), from = CROCONAW at 30)
    val SENTRET = add(161, "Sentret", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val FURRET = add(162, "Furret", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = SENTRET at 15)
    val HOOTHOOT = add(163, "Hoothoot", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val NOCTOWL = add(164, "Noctowl", Type.NORMAL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = HOOTHOOT at 20)
    val LEDYBA = add(165, "Ledyba", Type.BUG, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val LEDIAN = add(166, "Ledian", Type.BUG, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = LEDYBA at 18)
    val SPINARAK = add(167, "Spinarak", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val ARIADOS = add(168, "Ariados", Type.BUG, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = SPINARAK at 22)
    val CROBAT = add(169, "Crobat", Type.POISON, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = GOLBAT.byHappiness)
    val CHINCHOU = add(170, "Chinchou", Type.WATER, Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2))
    val LANTURN = add(171, "Lanturn", Type.WATER, Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2), from = CHINCHOU at 27)
    val TOGEPI = add(175, "Togepi", Type.FAIRY, genderRatio = 0.125, eggGroups = listOf(EggGroup.NO_EGGS))
    val TOGETIC = add(176, "Togetic", Type.FAIRY, Type.FLYING, genderRatio = 0.125, eggGroups = listOf(EggGroup.FLYING, EggGroup.FAIRY), from = TOGEPI.byHappiness)
    val NATU = add(177, "Natu", Type.PSYCHIC, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val XATU = add(178, "Xatu", Type.PSYCHIC, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING), from = NATU at 25)
    val MAREEP = add(179, "Mareep", Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD))
    val FLAAFFY = add(180, "Flaaffy", Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD), from = MAREEP at 15)
    val AMPHAROS = add(181, "Ampharos", Type.ELECTRIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.FIELD), from = FLAAFFY at 30)
    val BELLOSSOM = add(182, "Bellossom", Type.GRASS, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = GLOOM with Item.SUN_STONE)
    val MARILL = add(183, "Marill", Type.WATER, Type.FAIRY, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FAIRY))
    val AZUMARILL = add(184, "Azumarill", Type.WATER, Type.FAIRY, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FAIRY), from = MARILL at 18)
    val SUDOWOODO = add(185, "Sudowoodo", Type.ROCK, genderRatio = 0.5, eggGroups = listOf(EggGroup.MINERAL))
    val POLITOED = add(186, "Politoed", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1), from = POLIWHIRL.byTrade)
    val HOPPIP = add(187, "Hoppip", Type.GRASS, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FAIRY, EggGroup.GRASS))
    val SKIPLOOM = add(188, "Skiploom", Type.GRASS, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FAIRY, EggGroup.GRASS), from = HOPPIP at 18)
    val JUMPLUFF = add(189, "Jumpluff", Type.GRASS, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FAIRY, EggGroup.GRASS), from = SKIPLOOM at 27)
    val AIPOM = add(190, "Aipom", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val SUNKERN = add(191, "Sunkern", Type.GRASS, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS))
    val SUNFLORA = add(192, "Sunflora", Type.GRASS, genderRatio = 0.5, eggGroups = listOf(EggGroup.GRASS), from = SUNKERN with Item.SUN_STONE)
    val YANMA = add(193, "Yanma", Type.BUG, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val WOOPER = add(194, "Wooper", Type.WATER, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD))
    val QUAGSIRE = add(195, "Quagsire", Type.WATER, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD), from = WOOPER at 20)
    val ESPEON = add(196, "Espeon", Type.PSYCHIC, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = EEVEE.byDayHappiness)
    val UMBREON = add(197, "Umbreon", Type.DARK, genderRatio = 0.125, eggGroups = listOf(EggGroup.FIELD), from = EEVEE.byNightHappiness)
    val MURKROW = add(198, "Murkrow", Type.DARK, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val SLOWKING = add(199, "Slowking", Type.WATER, Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER, EggGroup.WATER1), from = SLOWPOKE.byTrade)
    val MISDREAVUS = add(200, "Misdreavus", Type.GHOST, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS))
    val UNOWN = add(201, "Unown", Type.PSYCHIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val WOBBUFFET = add(202, "Wobbuffet", Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS))
    val GIRAFARIG = add(203, "Girafarig", Type.NORMAL, Type.PSYCHIC, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val PINECO = add(204, "Pineco", Type.BUG, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val FORRETRESS = add(205, "Forretress", Type.BUG, Type.STEEL, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = PINECO at 31)
    val DUNSPARCE = add(206, "Dunsparce", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val GLIGAR = add(207, "Gligar", Type.GROUND, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val STEELIX = add(208, "Steelix", Type.STEEL, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MINERAL), from = ONIX.byTrade)
    val SNUBBULL = add(209, "Snubbull", Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.FIELD, EggGroup.FAIRY))
    val GRANBULL = add(210, "Granbull", Type.FAIRY, genderRatio = 0.75, eggGroups = listOf(EggGroup.FIELD, EggGroup.FAIRY), from = SNUBBULL at 23)
    val QWILFISH = add(211, "Qwilfish", Type.WATER, Type.POISON, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER2))
    val SCIZOR = add(212, "Scizor", Type.BUG, Type.STEEL, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG), from = SCYTHER.byTrade)
    val SHUCKLE = add(213, "Shuckle", Type.BUG, Type.ROCK, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val HERACROSS = add(214, "Heracross", Type.BUG, Type.FIGHTING, genderRatio = 0.5, eggGroups = listOf(EggGroup.BUG))
    val SNEASEL = add(215, "Sneasel", Type.DARK, Type.ICE, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val TEDDIURSA = add(216, "Teddiursa", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val URSARING = add(217, "Ursaring", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = TEDDIURSA at 30)
    val SLUGMA = add(218, "Slugma", Type.FIRE, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS))
    val MAGCARGO = add(219, "Magcargo", Type.FIRE, Type.ROCK, genderRatio = 0.5, eggGroups = listOf(EggGroup.AMORPHOUS), from = SLUGMA at 38)
    val SWINUB = add(220, "Swinub", Type.ICE, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val PILOSWINE = add(221, "Piloswine", Type.ICE, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = SWINUB at 33)
    val CORSOLA = add(222, "Corsola", Type.WATER, Type.ROCK, genderRatio = 0.75, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER3))
    val REMORAID = add(223, "Remoraid", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER2))
    val OCTILLERY = add(224, "Octillery", Type.WATER, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.WATER2), from = REMORAID at 25)
    val DELIBIRD = add(225, "Delibird", Type.ICE, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.FIELD))
    val MANTINE = add(226, "Mantine", Type.WATER, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1))
    val SKARMORY = add(227, "Skarmory", Type.STEEL, Type.FLYING, genderRatio = 0.5, eggGroups = listOf(EggGroup.FLYING))
    val HOUNDOUR = add(228, "Houndour", Type.DARK, Type.FIRE, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val HOUNDOOM = add(229, "Houndoom", Type.DARK, Type.FIRE, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = HOUNDOUR at 24)
    val KINGDRA = add(230, "Kingdra", Type.WATER, Type.DRAGON, genderRatio = 0.5, eggGroups = listOf(EggGroup.WATER1, EggGroup.DRAGON), from = SEADRA.byTrade)
    val PHANPY = add(231, "Phanpy", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val DONPHAN = add(232, "Donphan", Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD), from = PHANPY at 25)
    val PORYGON2 = add(233, "Porygon2", Type.NORMAL, genderRatio = -1.0, eggGroups = listOf(EggGroup.MINERAL), from = PORYGON.byTrade)
    val STANTLER = add(234, "Stantler", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val SMEARGLE = add(235, "Smeargle", Type.NORMAL, genderRatio = 0.5, eggGroups = listOf(EggGroup.FIELD))
    val HITMONTOP = add(237, "Hitmontop", Type.FIGHTING, genderRatio = 0.0, eggGroups = listOf(EggGroup.HUMAN_LIKE), from = TYROGUE at 20)
    val MILTANK = add(241, "Miltank", Type.NORMAL, genderRatio = 1.0, eggGroups = listOf(EggGroup.FIELD))
    val BLISSEY = add(242, "Blissey", Type.NORMAL, genderRatio = 1.0, eggGroups = listOf(EggGroup.FAIRY), from = CHANSEY.byHappiness)
    val RAIKOU = add(243, "Raikou", Type.ELECTRIC, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val ENTEI = add(244, "Entei", Type.FIRE, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val SUICUNE = add(245, "Suicune", Type.WATER, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val LARVITAR = add(246, "Larvitar", Type.ROCK, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER))
    val PUPITAR = add(247, "Pupitar", Type.ROCK, Type.GROUND, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER), from = LARVITAR at 30)
    val TYRANITAR = add(248, "Tyranitar", Type.ROCK, Type.DARK, genderRatio = 0.5, eggGroups = listOf(EggGroup.MONSTER), from = PUPITAR at 55)
    val LUGIA = add(249, "Lugia", Type.PSYCHIC, Type.FLYING, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val HO_OH = add(250, "Ho-Oh", Type.FIRE, Type.FLYING, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))
    val CELEBI = add(251, "Celebi", Type.PSYCHIC, Type.GRASS, genderRatio = -1.0, eggGroups = listOf(EggGroup.NO_EGGS))

    operator fun get(id: Int): PokemonSpecies? = entries[id]
    val all: Map<Int, PokemonSpecies> get() = entries.toMap()
}
