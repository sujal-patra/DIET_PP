package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY category ASC, name ASC")
    fun getAllPantryItems(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem)

    @Delete
    suspend fun deletePantryItem(item: PantryItem)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface DietLogDao {
    @Query("SELECT * FROM diet_logs ORDER BY timestamp DESC")
    fun getAllDietLogs(): Flow<List<DietLog>>

    @Query("SELECT * FROM diet_logs WHERE dateString = :date ORDER BY timestamp DESC")
    fun getDietLogsByDate(date: String): Flow<List<DietLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietLog(log: DietLog)

    @Delete
    suspend fun deleteDietLog(log: DietLog)

    @Query("DELETE FROM diet_logs WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface UserGoalDao {
    @Query("SELECT * FROM user_goals WHERE id = 1 LIMIT 1")
    fun getUserGoalFlow(): Flow<UserGoal?>

    @Query("SELECT * FROM user_goals WHERE id = 1 LIMIT 1")
    suspend fun getUserGoal(): UserGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGoal(goal: UserGoal)
}

@Database(
    entities = [PantryItem::class, DietLog::class, UserGoal::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun dietLogDao(): DietLogDao
    abstract fun userGoalDao(): UserGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diet_pantry_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
