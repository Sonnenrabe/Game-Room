package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val activeGames: Flow<List<Game>> = gameDao.getActiveGames()
    val archivedGames: Flow<List<Game>> = gameDao.getArchivedGames()

    fun getGameById(id: Long): Flow<Game?> = gameDao.getGameById(id)

    suspend fun getGameByIdDirect(id: Long): Game? = gameDao.getGameByIdDirect(id)

    suspend fun getAllGamesDirect(): List<Game> = gameDao.getAllGamesDirect()

    suspend fun insertGame(game: Game): Long = gameDao.insertGame(game)

    suspend fun insertAll(games: List<Game>) = gameDao.insertAll(games)

    suspend fun updateGame(game: Game) = gameDao.updateGame(game)

    suspend fun updateAll(games: List<Game>) = gameDao.updateAll(games)

    suspend fun deleteGame(game: Game) = gameDao.deleteGame(game)

    suspend fun deleteGameById(id: Long) = gameDao.deleteGameById(id)

    suspend fun clearAllGames() = gameDao.clearAllGames()

    suspend fun setArchivedStatus(id: Long, isArchived: Boolean) {
        gameDao.setArchivedStatus(id, isArchived, System.currentTimeMillis())
    }

    suspend fun updatePlaytime(id: Long, playtimeSeconds: Long) {
        gameDao.updatePlaytime(id, playtimeSeconds, System.currentTimeMillis())
    }

    suspend fun updateNotes(id: Long, notes: String) {
        gameDao.updateNotes(id, notes)
    }

    suspend fun updateSortOrder(id: Long, sortOrder: Int) {
        gameDao.updateSortOrder(id, sortOrder)
    }

    suspend fun importGames(games: List<Game>, replaceExisting: Boolean) {
        if (replaceExisting) {
            gameDao.clearAllGames()
        }
        gameDao.insertAll(games)
    }
}
