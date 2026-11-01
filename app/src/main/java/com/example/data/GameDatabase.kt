package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Game::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "games_playtime.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialGames(database.gameDao())
                    }
                }
            }

            private suspend fun populateInitialGames(gameDao: GameDao) {
                val now = System.currentTimeMillis()
                val initialGames = listOf(
                    Game(
                        title = "Elden Ring",
                        platform = "PC",
                        genre = "Action RPG",
                        totalPlaytimeSeconds = 84L * 3600L + 22L * 60L + 15L, // 84h 22m 15s
                        isArchived = false,
                        colorHex = 0xFFFFAB00,
                        createdAt = now - (30L * 86400000L),
                        lastPlayedAt = now - 3600000L,
                        notes = "Exploring Leyndell Capital"
                    ),
                    Game(
                        title = "Metroid Prime Remastered",
                        platform = "Nintendo Switch",
                        genre = "Action Adventure",
                        totalPlaytimeSeconds = 12L * 3600L + 45L * 60L + 30L, // 12h 45m 30s
                        isArchived = false,
                        colorHex = 0xFF00E5FF,
                        createdAt = now - (14L * 86400000L),
                        lastPlayedAt = now - 86400000L,
                        notes = "Acquired Gravity Suit"
                    ),
                    Game(
                        title = "Hades II",
                        platform = "Steam Deck",
                        genre = "Roguelike",
                        totalPlaytimeSeconds = 29L * 3600L + 10L * 60L + 4L, // 29h 10m 04s
                        isArchived = false,
                        colorHex = 0xFFFF4081,
                        createdAt = now - (7L * 86400000L),
                        lastPlayedAt = now - 1800000L,
                        notes = "Night of the Underworld runs"
                    ),
                    Game(
                        title = "Chrono Trigger",
                        platform = "Retro",
                        genre = "JRPG",
                        totalPlaytimeSeconds = 36L * 3600L + 50L * 60L + 0L, // 36h 50m 00s
                        isArchived = true,
                        colorHex = 0xFF7C4DFF,
                        createdAt = now - (60L * 86400000L),
                        lastPlayedAt = now - (10L * 86400000L),
                        notes = "Completed all endings!"
                    )
                )
                gameDao.insertAll(initialGames)
            }
        }
    }
}
