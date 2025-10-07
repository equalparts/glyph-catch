package dev.equalparts.glyph_catch.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Pokémon caught by the user.
 */
@Entity(tableName = "caught_pokemon")
data class CaughtPokemon(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val speciesId: Int,
    val caughtAt: Long = System.currentTimeMillis(),
    val nickname: String? = null,
    val isFavorite: Boolean = false,
    val isTraining: Boolean = false,
    @ColumnInfo(name = "currentExp") val exp: Int = 0,
    val level: Int = 1,
    val spawnPoolName: String? = null,
    val isSpecialSpawn: Boolean = false,
    val isConditionalSpawn: Boolean = false,
    @ColumnInfo(defaultValue = "0") val screenOffDurationMinutes: Int = 0
)

/**
 * Pokédex record. One per species ID.
 */
@Entity(tableName = "pokedex_records")
data class PokedexRecord(
    @PrimaryKey val speciesId: Int,
    val caught: Boolean = false,
    val timesCaught: Int = 0,
    val firstCaughtAt: Long? = null
)

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
    @Query("SELECT * FROM caught_pokemon ORDER BY speciesId")
    fun watchAllCaught(): Flow<List<CaughtPokemon>>

    @Query("SELECT * FROM caught_pokemon WHERE id = :pokemonId LIMIT 1")
    fun watchCaughtPokemon(pokemonId: String): Flow<CaughtPokemon?>

    @Query("SELECT * FROM caught_pokemon ORDER BY caughtAt DESC LIMIT :limit")
    fun watchRecentCatches(limit: Int = 5): Flow<List<CaughtPokemon>>

    @Query("SELECT COUNT(*) FROM caught_pokemon WHERE date(caughtAt/1000, 'unixepoch') = date('now')")
    fun watchCaughtTodayCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM caught_pokemon")
    fun watchTotalCaughtCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT speciesId) FROM caught_pokemon")
    fun watchPokedexProgress(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT speciesId) FROM caught_pokemon")
    suspend fun getUniqueSpeciesCount(): Int

    @Query("SELECT COUNT(*) FROM caught_pokemon WHERE speciesId = :speciesId")
    suspend fun countBySpeciesId(speciesId: Int): Int

    @Insert
    suspend fun insert(pokemon: CaughtPokemon)

    @Query("UPDATE caught_pokemon SET isFavorite = :isFavorite WHERE id = :pokemonId")
    suspend fun updateFavorite(pokemonId: String, isFavorite: Boolean)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getItem(itemId: Int): InventoryItem?

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
    entities = [CaughtPokemon::class, PokedexRecord::class, InventoryItem::class, ActiveItem::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun activeItemDao(): ActiveItemDao

    companion object {
        @Volatile
        private var INSTANCE: PokemonDatabase? = null

        fun getInstance(context: Context): PokemonDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                PokemonDatabase::class.java,
                "pokemon.db"
            )
                .fallbackToDestructiveMigration(false)
                .build().also { INSTANCE = it }
        }
    }
}
