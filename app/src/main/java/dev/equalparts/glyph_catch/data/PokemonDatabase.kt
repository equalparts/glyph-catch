package dev.equalparts.glyph_catch.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.equalparts.glyph_catch.debug.DebugEvent
import dev.equalparts.glyph_catch.debug.DebugEventDao
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Pokémon genders.
 */
enum class Gender {
    MALE, FEMALE, GENDERLESS
}

/**
 * Pokémon caught by the user.
 */
@Entity(tableName = "caught_pokemon")
data class CaughtPokemon(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val speciesId: Int,
    val caughtAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val spawnedAt: Long = 0L,
    val nickname: String? = null,
    val isFavorite: Boolean = false,
    val isTraining: Boolean = false,
    @ColumnInfo(defaultValue = "0") val trainingSlot: Int = 0,
    @ColumnInfo(name = "currentExp") val exp: Int = 0,
    val level: Int = 1,
    val spawnPoolName: String? = null,
    val isSpecialSpawn: Boolean = false,
    val isConditionalSpawn: Boolean = false,
    @ColumnInfo(defaultValue = "0") val screenOffDurationMinutes: Int = 0,
    @ColumnInfo(defaultValue = "GENDERLESS") val gender: Gender = Gender.GENDERLESS,
    @ColumnInfo(defaultValue = "0") val happiness: Int = 0,
    @ColumnInfo(defaultValue = "0") val isEgg: Boolean = false,
    @ColumnInfo(defaultValue = "0") val steps: Int = 0,
    @ColumnInfo(defaultValue = "0") val requiredSteps: Int = 0,
    val variant: String? = null
)

/**
 * Pokédex record. One per species ID.
 */
@Entity(tableName = "pokedex_records")
data class PokedexRecord(@PrimaryKey val speciesId: Int)

/**
 * Item collected by the user.
 */
@Entity(tableName = "inventory")
data class InventoryItem(@PrimaryKey val itemId: Int, val quantity: Int = 0)

/**
 * Item used by the user.
 */
@Entity(tableName = "active_items")
data class ActiveItem(
    @PrimaryKey val itemId: Int,
    val activatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (60 * 60 * 1000)
)

@Dao
interface PokemonDao {
    @Query("SELECT * FROM caught_pokemon WHERE trainingSlot != 0 ORDER BY trainingSlot ASC")
    fun watchTrainingPartners(): Flow<List<CaughtPokemon>>

    @Query("SELECT * FROM caught_pokemon WHERE trainingSlot != 0 ORDER BY trainingSlot ASC")
    suspend fun getActiveTrainingPartners(): List<CaughtPokemon>

    @Query("SELECT * FROM caught_pokemon WHERE trainingSlot = 1 LIMIT 1")
    fun watchTrainingPartner(): Flow<CaughtPokemon?>

    @Query("SELECT * FROM caught_pokemon WHERE trainingSlot = 1 LIMIT 1")
    suspend fun getActiveTrainingPartner(): CaughtPokemon?

    @Query("UPDATE caught_pokemon SET trainingSlot = 0, isTraining = 0 WHERE trainingSlot = :slot")
    suspend fun clearTrainingSlot(slot: Int)

    @Query("UPDATE caught_pokemon SET trainingSlot = 0, isTraining = 0")
    suspend fun clearAllTrainingPartners()

    @Query("UPDATE caught_pokemon SET trainingSlot = 0, isTraining = 0 WHERE id = :pokemonId")
    suspend fun stopTraining(pokemonId: String)

    @Query("UPDATE caught_pokemon SET trainingSlot = :slot, isTraining = 1 WHERE id = :pokemonId")
    suspend fun updateTrainingSlot(pokemonId: String, slot: Int)

    @Transaction
    suspend fun setActiveTrainingPartner(pokemonId: String, slot: Int) {
        clearTrainingSlot(slot)
        stopTraining(pokemonId)
        updateTrainingSlot(pokemonId, slot)
    }

    @Query("UPDATE caught_pokemon SET isTraining = 0")
    suspend fun clearTrainingPartner()

    @Query("UPDATE caught_pokemon SET isTraining = CASE WHEN id = :pokemonId THEN 1 ELSE 0 END, trainingSlot = CASE WHEN id = :pokemonId THEN 1 ELSE 0 END")
    suspend fun setActiveTrainingPartner(pokemonId: String)

    @Query("UPDATE caught_pokemon SET currentExp = :exp, level = :level WHERE id = :pokemonId")
    suspend fun updateTrainingProgress(pokemonId: String, exp: Int, level: Int)

    @Query("UPDATE caught_pokemon SET happiness = :happiness WHERE id = :pokemonId")
    suspend fun updateHappiness(pokemonId: String, happiness: Int)

    @Query(
        "UPDATE caught_pokemon SET speciesId = :newSpeciesId, level = :newLevel, currentExp = :newExp WHERE id = :pokemonId"
    )
    suspend fun evolvePokemon(pokemonId: String, newSpeciesId: Int, newLevel: Int, newExp: Int)

    @Query("SELECT MAX(caughtAt) FROM caught_pokemon WHERE speciesId IN (:speciesIds)")
    suspend fun getLastCaughtAtForSpecies(speciesIds: List<Int>): Long?

    @Query("SELECT * FROM caught_pokemon ORDER BY speciesId")
    fun watchAllCaught(): Flow<List<CaughtPokemon>>

    @Query("SELECT * FROM caught_pokemon")
    suspend fun getAllCaughtList(): List<CaughtPokemon>

    @Query("SELECT * FROM caught_pokemon WHERE id = :pokemonId LIMIT 1")
    fun watchCaughtPokemon(pokemonId: String): Flow<CaughtPokemon?>

    @Query("SELECT * FROM caught_pokemon WHERE id = :pokemonId LIMIT 1")
    suspend fun getCaughtPokemon(pokemonId: String): CaughtPokemon?

    @Query("SELECT * FROM caught_pokemon ORDER BY caughtAt DESC LIMIT :limit")
    fun watchRecentCatches(limit: Int = 5): Flow<List<CaughtPokemon>>

    @Query("SELECT COUNT(*) FROM caught_pokemon")
    suspend fun getTotalCaughtCount(): Int

    @Query("SELECT COUNT(*) FROM caught_pokemon WHERE date(caughtAt/1000, 'unixepoch') = date('now')")
    fun watchCaughtTodayCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM caught_pokemon")
    fun watchTotalCaughtCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pokedex_records")
    fun watchPokedexProgress(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pokedex_records")
    suspend fun getUniqueSpeciesCount(): Int

    @Query("SELECT COUNT(*) FROM pokedex_records WHERE speciesId <= 151")
    suspend fun getKantoPokedexCount(): Int

    @Query("SELECT speciesId FROM pokedex_records ORDER BY speciesId")
    fun watchCaughtSpeciesIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM caught_pokemon WHERE speciesId = :speciesId)")
    suspend fun hasInInventory(speciesId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM caught_pokemon WHERE speciesId = :speciesId AND variant = :variant)")
    suspend fun hasVariantInInventory(speciesId: Int, variant: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM pokedex_records WHERE speciesId = :speciesId)")
    suspend fun hasPokedexEntry(speciesId: Int): Boolean

    @Query("SELECT * FROM pokedex_records WHERE speciesId = :speciesId LIMIT 1")
    suspend fun getPokedexRecord(speciesId: Int): PokedexRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPokedexRecord(record: PokedexRecord)

    @Transaction
    suspend fun recordPokedexEntry(speciesId: Int) {
        if (!hasPokedexEntry(speciesId)) {
            upsertPokedexRecord(PokedexRecord(speciesId = speciesId))
        }
    }

    @Query(
        "SELECT MAX(CASE WHEN spawnedAt > 0 THEN spawnedAt ELSE caughtAt END) FROM caught_pokemon WHERE spawnPoolName = :poolName"
    )
    suspend fun getLastSpawnedAtForPool(poolName: String): Long?

    @Insert
    suspend fun insert(pokemon: CaughtPokemon)

    @Query("UPDATE caught_pokemon SET isFavorite = :isFavorite WHERE id = :pokemonId")
    suspend fun updateFavorite(pokemonId: String, isFavorite: Boolean)

    @Query("UPDATE caught_pokemon SET steps = steps + :amount WHERE id = :pokemonId")
    suspend fun addSteps(pokemonId: String, amount: Int)

    @Query("UPDATE caught_pokemon SET isEgg = 0 WHERE id = :pokemonId")
    suspend fun hatchEgg(pokemonId: String)

    @Query("UPDATE caught_pokemon SET gender = :gender WHERE id = :pokemonId")
    suspend fun updateGender(pokemonId: String, gender: Gender)

    @Transaction
    suspend fun addStepsAndCheckHatch(pokemonId: String, amount: Int): Int? {
        addSteps(pokemonId, amount)
        val p = getCaughtPokemon(pokemonId)
        if (p != null && p.isEgg && p.steps >= p.requiredSteps && p.requiredSteps > 0) {
            hatchEgg(pokemonId)
            return p.speciesId
        }
        return null
    }

    @Query("SELECT COUNT(*) FROM caught_pokemon WHERE level >= :minLevel")
    suspend fun countPokemonAtLevel(minLevel: Int): Int
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getItem(itemId: Int): InventoryItem?

    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    fun watchItem(itemId: Int): Flow<InventoryItem?>

    @Query("SELECT * FROM inventory")
    fun watchAllItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: InventoryItem)

    @Query("UPDATE inventory SET quantity = quantity + 1 WHERE itemId = :itemId")
    suspend fun addItem(itemId: Int)

    @Query("UPDATE inventory SET quantity = quantity + :amount WHERE itemId = :itemId")
    suspend fun addItems(itemId: Int, amount: Int)

    @Query("UPDATE inventory SET quantity = quantity - 1 WHERE itemId = :itemId AND quantity > 0")
    suspend fun useItem(itemId: Int)
}

@Dao
interface ActiveItemDao {
    @Query("SELECT * FROM active_items WHERE itemId = :itemId AND expiresAt > :now")
    suspend fun getActiveItem(itemId: Int, now: Long = System.currentTimeMillis()): ActiveItem?

    @Query("SELECT * FROM active_items WHERE itemId = :itemId")
    fun watchActiveItem(itemId: Int): Flow<ActiveItem?>

    @Query("SELECT * FROM active_items WHERE expiresAt > :now")
    suspend fun getAllActiveItems(now: Long = System.currentTimeMillis()): List<ActiveItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun activateItem(item: ActiveItem)

    @Query("DELETE FROM active_items WHERE expiresAt <= :now")
    suspend fun cleanupExpiredItems(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM active_items WHERE itemId = :itemId")
    suspend fun deactivateItem(itemId: Int)
}

@Database(
    entities = [
        CaughtPokemon::class,
        PokedexRecord::class,
        InventoryItem::class,
        ActiveItem::class,
        DebugEvent::class
    ],
    version = 17,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = DebugEventsMigration6To7::class),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 16, to = 17)
    ]
)
abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun activeItemDao(): ActiveItemDao
    abstract fun debugEventDao(): DebugEventDao

    companion object {
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caught_pokemon ADD COLUMN requiredSteps INTEGER NOT NULL DEFAULT 0")
                // Assign random steps (5-20km, ~1300 steps/km) to existing eggs.
                // Range: 6500 to 26000 steps. Range size = 19501.
                db.execSQL("UPDATE caught_pokemon SET requiredSteps = ABS(RANDOM()) % 19501 + 6500 WHERE isEgg = 1")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caught_pokemon ADD COLUMN steps INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caught_pokemon ADD COLUMN isEgg INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caught_pokemon ADD COLUMN trainingSlot INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE caught_pokemon SET trainingSlot = 1 WHERE isTraining = 1")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caught_pokemon ADD COLUMN happiness INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the gender column
                db.execSQL("ALTER TABLE caught_pokemon ADD COLUMN gender TEXT NOT NULL DEFAULT 'GENDERLESS'")

                // Assign random genders to existing Pokémon
                val cursor = db.query("SELECT id, speciesId FROM caught_pokemon")
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val speciesId = cursor.getInt(1)
                    val species = Pokemon[speciesId]
                    if (species != null && species.genderRatio != -1.0) {
                        val gender = if (java.util.Random().nextDouble() < species.genderRatio) {
                            Gender.FEMALE.name
                        } else {
                            Gender.MALE.name
                        }
                        db.execSQL("UPDATE caught_pokemon SET gender = '$gender' WHERE id = '$id'")
                    }
                }
                cursor.close()
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO pokedex_records (speciesId, caught, timesCaught, firstCaughtAt)
                    SELECT speciesId, 1, COUNT(*), MIN(caughtAt)
                    FROM caught_pokemon
                    GROUP BY speciesId
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE pokedex_records
                    SET timesCaught = (
                        SELECT COUNT(*) FROM caught_pokemon WHERE caught_pokemon.speciesId = pokedex_records.speciesId
                    )
                    WHERE speciesId IN (SELECT speciesId FROM caught_pokemon)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE pokedex_records
                    SET caught = 1
                    WHERE speciesId IN (SELECT speciesId FROM caught_pokemon)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE pokedex_records
                    SET firstCaughtAt = (
                        SELECT MIN(caughtAt)
                        FROM caught_pokemon
                        WHERE caught_pokemon.speciesId = pokedex_records.speciesId
                    )
                    WHERE firstCaughtAt IS NULL AND speciesId IN (SELECT speciesId FROM caught_pokemon)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pokedex_records_new (
                        speciesId INTEGER NOT NULL,
                        PRIMARY KEY(speciesId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO pokedex_records_new (speciesId)
                    SELECT DISTINCT speciesId FROM pokedex_records
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE pokedex_records")
                db.execSQL("ALTER TABLE pokedex_records_new RENAME TO pokedex_records")
            }
        }

        @Volatile
        private var INSTANCE: PokemonDatabase? = null

        fun getInstance(context: Context): PokemonDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                PokemonDatabase::class.java,
                "pokemon.db"
            )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                .fallbackToDestructiveMigration(false)
                .build().also { INSTANCE = it }
        }
    }
}

@RenameColumn.Entries(
    RenameColumn("debug_events", "batteryPercent", "phoneBattery"),
    RenameColumn("debug_events", "isInteractive", "phoneIsInteractive"),
    RenameColumn("debug_events", "minutesScreenOff", "phoneMinutesOff"),
    RenameColumn("debug_events", "minutesScreenOffForSpawn", "phoneMinutesOffOutsideBedtime"),
    RenameColumn("debug_events", "isDuringSleepWindow", "isBedtime")
)
@DeleteColumn.Entries(DeleteColumn("debug_events", "sleepMinutesOutside"))
class DebugEventsMigration6To7 : AutoMigrationSpec
