package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE isArchived = 0 ORDER BY lastPlayedAt DESC, id DESC")
    fun getActiveGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE isArchived = 1 ORDER BY lastPlayedAt DESC, id DESC")
    fun getArchivedGames(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY id ASC")
    suspend fun getAllGamesDirect(): List<Game>

    @Query("SELECT * FROM games WHERE id = :id")
    fun getGameById(id: Long): Flow<Game?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameByIdDirect(id: Long): Game?

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<Game>)

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("DELETE FROM games")
    suspend fun clearAllGames()

    @Query("UPDATE games SET isArchived = :isArchived, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun setArchivedStatus(id: Long, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE games SET totalPlaytimeSeconds = :playtimeSeconds, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updatePlaytime(id: Long, playtimeSeconds: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE games SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)
}
