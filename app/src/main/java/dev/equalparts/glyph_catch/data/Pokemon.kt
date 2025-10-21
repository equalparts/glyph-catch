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
    val evolutionRequirement: EvolutionRequirement? = null
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
 * Evolution requirement definition for a [PokemonSpecies].
 */
sealed class EvolutionRequirement {
    data class Level(val level: Int) : EvolutionRequirement()
    data class Stone(val item: Item) : EvolutionRequirement()
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
    REPEL
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
    val PokemonSpecies.byTrade get() = EvolutionFrom(this, EvolutionRequirement.Trade)

    private fun add(
        id: Int,
        name: String,
        type1: Type,
        type2: Type? = null,
        from: EvolutionFrom? = null
    ): PokemonSpecies {
        val species = PokemonSpecies(id, name, type1, type2, evolutionRequirement = from?.requirement)
        entries[id] = species
        from?.source?.evolvesTo?.add(id)
        return species
    }

    val BULBASAUR = add(1, "Bulbasaur", Type.GRASS, Type.POISON)
    val IVYSAUR = add(2, "Ivysaur", Type.GRASS, Type.POISON, from = BULBASAUR at 16)
    val VENUSAUR = add(3, "Venusaur", Type.GRASS, Type.POISON, from = IVYSAUR at 32)
    val CHARMANDER = add(4, "Charmander", Type.FIRE)
    val CHARMELEON = add(5, "Charmeleon", Type.FIRE, from = CHARMANDER at 16)
    val CHARIZARD = add(6, "Charizard", Type.FIRE, Type.FLYING, from = CHARMELEON at 36)
    val SQUIRTLE = add(7, "Squirtle", Type.WATER)
    val WARTORTLE = add(8, "Wartortle", Type.WATER, from = SQUIRTLE at 16)
    val BLASTOISE = add(9, "Blastoise", Type.WATER, from = WARTORTLE at 36)
    val CATERPIE = add(10, "Caterpie", Type.BUG)
    val METAPOD = add(11, "Metapod", Type.BUG, from = CATERPIE at 7)
    val BUTTERFREE = add(12, "Butterfree", Type.BUG, Type.FLYING, from = METAPOD at 10)
    val WEEDLE = add(13, "Weedle", Type.BUG, Type.POISON)
    val KAKUNA = add(14, "Kakuna", Type.BUG, Type.POISON, from = WEEDLE at 7)
    val BEEDRILL = add(15, "Beedrill", Type.BUG, Type.POISON, from = KAKUNA at 10)
    val PIDGEY = add(16, "Pidgey", Type.NORMAL, Type.FLYING)
    val PIDGEOTTO = add(17, "Pidgeotto", Type.NORMAL, Type.FLYING, from = PIDGEY at 18)
    val PIDGEOT = add(18, "Pidgeot", Type.NORMAL, Type.FLYING, from = PIDGEOTTO at 36)
    val RATTATA = add(19, "Rattata", Type.NORMAL)
    val RATICATE = add(20, "Raticate", Type.NORMAL, from = RATTATA at 20)
    val SPEAROW = add(21, "Spearow", Type.NORMAL, Type.FLYING)
    val FEAROW = add(22, "Fearow", Type.NORMAL, Type.FLYING, from = SPEAROW at 20)
    val EKANS = add(23, "Ekans", Type.POISON)
    val ARBOK = add(24, "Arbok", Type.POISON, from = EKANS at 22)
    val PIKACHU = add(25, "Pikachu", Type.ELECTRIC)
    val RAICHU = add(26, "Raichu", Type.ELECTRIC, from = PIKACHU with Item.THUNDER_STONE)
    val SANDSHREW = add(27, "Sandshrew", Type.GROUND)
    val SANDSLASH = add(28, "Sandslash", Type.GROUND, from = SANDSHREW at 22)
    val NIDORAN_F = add(29, "Nidoran♀", Type.POISON)
    val NIDORINA = add(30, "Nidorina", Type.POISON, from = NIDORAN_F at 16)
    val NIDOQUEEN = add(31, "Nidoqueen", Type.POISON, Type.GROUND, from = NIDORINA with Item.MOON_STONE)
    val NIDORAN_M = add(32, "Nidoran♂", Type.POISON)
    val NIDORINO = add(33, "Nidorino", Type.POISON, from = NIDORAN_M at 16)
    val NIDOKING = add(34, "Nidoking", Type.POISON, Type.GROUND, from = NIDORINO with Item.MOON_STONE)
    val CLEFAIRY = add(35, "Clefairy", Type.FAIRY)
    val CLEFABLE = add(36, "Clefable", Type.FAIRY, from = CLEFAIRY with Item.MOON_STONE)
    val VULPIX = add(37, "Vulpix", Type.FIRE)
    val NINETALES = add(38, "Ninetales", Type.FIRE, from = VULPIX with Item.FIRE_STONE)
    val JIGGLYPUFF = add(39, "Jigglypuff", Type.NORMAL, Type.FAIRY)
    val WIGGLYTUFF = add(40, "Wigglytuff", Type.NORMAL, Type.FAIRY, from = JIGGLYPUFF with Item.MOON_STONE)
    val ZUBAT = add(41, "Zubat", Type.POISON, Type.FLYING)
    val GOLBAT = add(42, "Golbat", Type.POISON, Type.FLYING, from = ZUBAT at 22)
    val ODDISH = add(43, "Oddish", Type.GRASS, Type.POISON)
    val GLOOM = add(44, "Gloom", Type.GRASS, Type.POISON, from = ODDISH at 21)
    val VILEPLUME = add(45, "Vileplume", Type.GRASS, Type.POISON, from = GLOOM with Item.LEAF_STONE)
    val PARAS = add(46, "Paras", Type.BUG, Type.GRASS)
    val PARASECT = add(47, "Parasect", Type.BUG, Type.GRASS, from = PARAS at 24)
    val VENONAT = add(48, "Venonat", Type.BUG, Type.POISON)
    val VENOMOTH = add(49, "Venomoth", Type.BUG, Type.POISON, from = VENONAT at 31)
    val DIGLETT = add(50, "Diglett", Type.GROUND)
    val DUGTRIO = add(51, "Dugtrio", Type.GROUND, from = DIGLETT at 26)
    val MEOWTH = add(52, "Meowth", Type.NORMAL)
    val PERSIAN = add(53, "Persian", Type.NORMAL, from = MEOWTH at 28)
    val PSYDUCK = add(54, "Psyduck", Type.WATER)
    val GOLDUCK = add(55, "Golduck", Type.WATER, from = PSYDUCK at 33)
    val MANKEY = add(56, "Mankey", Type.FIGHTING)
    val PRIMEAPE = add(57, "Primeape", Type.FIGHTING, from = MANKEY at 28)
    val GROWLITHE = add(58, "Growlithe", Type.FIRE)
    val ARCANINE = add(59, "Arcanine", Type.FIRE, from = GROWLITHE with Item.FIRE_STONE)
    val POLIWAG = add(60, "Poliwag", Type.WATER)
    val POLIWHIRL = add(61, "Poliwhirl", Type.WATER, from = POLIWAG at 25)
    val POLIWRATH = add(62, "Poliwrath", Type.WATER, Type.FIGHTING, from = POLIWHIRL with Item.WATER_STONE)
    val ABRA = add(63, "Abra", Type.PSYCHIC)
    val KADABRA = add(64, "Kadabra", Type.PSYCHIC, from = ABRA at 16)
    val ALAKAZAM = add(65, "Alakazam", Type.PSYCHIC, from = KADABRA.byTrade)
    val MACHOP = add(66, "Machop", Type.FIGHTING)
    val MACHOKE = add(67, "Machoke", Type.FIGHTING, from = MACHOP at 28)
    val MACHAMP = add(68, "Machamp", Type.FIGHTING, from = MACHOKE.byTrade)
    val BELLSPROUT = add(69, "Bellsprout", Type.GRASS, Type.POISON)
    val WEEPINBELL = add(70, "Weepinbell", Type.GRASS, Type.POISON, from = BELLSPROUT at 21)
    val VICTREEBEL = add(71, "Victreebel", Type.GRASS, Type.POISON, from = WEEPINBELL with Item.LEAF_STONE)
    val TENTACOOL = add(72, "Tentacool", Type.WATER, Type.POISON)
    val TENTACRUEL = add(73, "Tentacruel", Type.WATER, Type.POISON, from = TENTACOOL at 30)
    val GEODUDE = add(74, "Geodude", Type.ROCK, Type.GROUND)
    val GRAVELER = add(75, "Graveler", Type.ROCK, Type.GROUND, from = GEODUDE at 25)
    val GOLEM = add(76, "Golem", Type.ROCK, Type.GROUND, from = GRAVELER.byTrade)
    val PONYTA = add(77, "Ponyta", Type.FIRE)
    val RAPIDASH = add(78, "Rapidash", Type.FIRE, from = PONYTA at 40)
    val SLOWPOKE = add(79, "Slowpoke", Type.WATER, Type.PSYCHIC)
    val SLOWBRO = add(80, "Slowbro", Type.WATER, Type.PSYCHIC, from = SLOWPOKE at 37)
    val MAGNEMITE = add(81, "Magnemite", Type.ELECTRIC, Type.STEEL)
    val MAGNETON = add(82, "Magneton", Type.ELECTRIC, Type.STEEL, from = MAGNEMITE at 30)
    val FARFETCHD = add(83, "Farfetch'd", Type.NORMAL, Type.FLYING)
    val DODUO = add(84, "Doduo", Type.NORMAL, Type.FLYING)
    val DODRIO = add(85, "Dodrio", Type.NORMAL, Type.FLYING, from = DODUO at 31)
    val SEEL = add(86, "Seel", Type.WATER)
    val DEWGONG = add(87, "Dewgong", Type.WATER, Type.ICE, from = SEEL at 34)
    val GRIMER = add(88, "Grimer", Type.POISON)
    val MUK = add(89, "Muk", Type.POISON, from = GRIMER at 38)
    val SHELLDER = add(90, "Shellder", Type.WATER)
    val CLOYSTER = add(91, "Cloyster", Type.WATER, Type.ICE, from = SHELLDER with Item.WATER_STONE)
    val GASTLY = add(92, "Gastly", Type.GHOST, Type.POISON)
    val HAUNTER = add(93, "Haunter", Type.GHOST, Type.POISON, from = GASTLY at 25)
    val GENGAR = add(94, "Gengar", Type.GHOST, Type.POISON, from = HAUNTER.byTrade)
    val ONIX = add(95, "Onix", Type.ROCK, Type.GROUND)
    val DROWZEE = add(96, "Drowzee", Type.PSYCHIC)
    val HYPNO = add(97, "Hypno", Type.PSYCHIC, from = DROWZEE at 26)
    val KRABBY = add(98, "Krabby", Type.WATER)
    val KINGLER = add(99, "Kingler", Type.WATER, from = KRABBY at 28)
    val VOLTORB = add(100, "Voltorb", Type.ELECTRIC)
    val ELECTRODE = add(101, "Electrode", Type.ELECTRIC, from = VOLTORB at 30)
    val EXEGGCUTE = add(102, "Exeggcute", Type.GRASS, Type.PSYCHIC)
    val EXEGGUTOR = add(103, "Exeggutor", Type.GRASS, Type.PSYCHIC, from = EXEGGCUTE with Item.LEAF_STONE)
    val CUBONE = add(104, "Cubone", Type.GROUND)
    val MAROWAK = add(105, "Marowak", Type.GROUND, from = CUBONE at 28)
    val HITMONLEE = add(106, "Hitmonlee", Type.FIGHTING)
    val HITMONCHAN = add(107, "Hitmonchan", Type.FIGHTING)
    val LICKITUNG = add(108, "Lickitung", Type.NORMAL)
    val KOFFING = add(109, "Koffing", Type.POISON)
    val WEEZING = add(110, "Weezing", Type.POISON, from = KOFFING at 35)
    val RHYHORN = add(111, "Rhyhorn", Type.GROUND, Type.ROCK)
    val RHYDON = add(112, "Rhydon", Type.GROUND, Type.ROCK, from = RHYHORN at 42)
    val CHANSEY = add(113, "Chansey", Type.NORMAL)
    val TANGELA = add(114, "Tangela", Type.GRASS)
    val KANGASKHAN = add(115, "Kangaskhan", Type.NORMAL)
    val HORSEA = add(116, "Horsea", Type.WATER)
    val SEADRA = add(117, "Seadra", Type.WATER, from = HORSEA at 32)
    val GOLDEEN = add(118, "Goldeen", Type.WATER)
    val SEAKING = add(119, "Seaking", Type.WATER, from = GOLDEEN at 33)
    val STARYU = add(120, "Staryu", Type.WATER)
    val STARMIE = add(121, "Starmie", Type.WATER, Type.PSYCHIC, from = STARYU with Item.WATER_STONE)
    val MR_MIME = add(122, "Mr. Mime", Type.PSYCHIC, Type.FAIRY)
    val SCYTHER = add(123, "Scyther", Type.BUG, Type.FLYING)
    val JYNX = add(124, "Jynx", Type.ICE, Type.PSYCHIC)
    val ELECTABUZZ = add(125, "Electabuzz", Type.ELECTRIC)
    val MAGMAR = add(126, "Magmar", Type.FIRE)
    val PINSIR = add(127, "Pinsir", Type.BUG)
    val TAUROS = add(128, "Tauros", Type.NORMAL)
    val MAGIKARP = add(129, "Magikarp", Type.WATER)
    val GYARADOS = add(130, "Gyarados", Type.WATER, Type.FLYING, from = MAGIKARP at 20)
    val LAPRAS = add(131, "Lapras", Type.WATER, Type.ICE)
    val DITTO = add(132, "Ditto", Type.NORMAL)
    val EEVEE = add(133, "Eevee", Type.NORMAL)
    val VAPOREON = add(134, "Vaporeon", Type.WATER, from = EEVEE with Item.WATER_STONE)
    val JOLTEON = add(135, "Jolteon", Type.ELECTRIC, from = EEVEE with Item.THUNDER_STONE)
    val FLAREON = add(136, "Flareon", Type.FIRE, from = EEVEE with Item.FIRE_STONE)
    val PORYGON = add(137, "Porygon", Type.NORMAL)
    val OMANYTE = add(138, "Omanyte", Type.ROCK, Type.WATER)
    val OMASTAR = add(139, "Omastar", Type.ROCK, Type.WATER, from = OMANYTE at 40)
    val KABUTO = add(140, "Kabuto", Type.ROCK, Type.WATER)
    val KABUTOPS = add(141, "Kabutops", Type.ROCK, Type.WATER, from = KABUTO at 40)
    val AERODACTYL = add(142, "Aerodactyl", Type.ROCK, Type.FLYING)
    val SNORLAX = add(143, "Snorlax", Type.NORMAL)
    val ARTICUNO = add(144, "Articuno", Type.ICE, Type.FLYING)
    val ZAPDOS = add(145, "Zapdos", Type.ELECTRIC, Type.FLYING)
    val MOLTRES = add(146, "Moltres", Type.FIRE, Type.FLYING)
    val DRATINI = add(147, "Dratini", Type.DRAGON)
    val DRAGONAIR = add(148, "Dragonair", Type.DRAGON, from = DRATINI at 30)
    val DRAGONITE = add(149, "Dragonite", Type.DRAGON, Type.FLYING, from = DRAGONAIR at 55)
    val MEWTWO = add(150, "Mewtwo", Type.PSYCHIC)
    val MEW = add(151, "Mew", Type.PSYCHIC)

    val DELIBIRD = add(225, "Delibird", Type.ICE, Type.FLYING)
    val STANTLER = add(234, "Stantler", Type.NORMAL)

    operator fun get(id: Int): PokemonSpecies? = entries[id]
    val all: Map<Int, PokemonSpecies> get() = entries.toMap()
}
