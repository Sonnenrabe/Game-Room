package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Game
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.notification.GameTimerSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val totalCount: Int,
    val activeCount: Int,
    val archivedCount: Int,
    val totalPlaytimeFormatted: String,
    val games: List<Game>
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository

    init {
        val database = GameDatabase.getInstance(application, viewModelScope)
        repository = GameRepository(database.gameDao())
    }

    // Tab Selection: 0 = Active, 1 = Archived
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    // Running Timer State synced with GameTimerSessionManager & Ongoing Push Notification
    val runningGameId: StateFlow<Long?> = GameTimerSessionManager.runningGameId
    val elapsedSessionSeconds: StateFlow<Long> = GameTimerSessionManager.elapsedSessionSeconds

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Raw streams from DB
    val allActiveGames: StateFlow<List<Game>> = repository.activeGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArchivedGames: StateFlow<List<Game>> = repository.archivedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists
    val filteredActiveGames: StateFlow<List<Game>> = combine(
        allActiveGames,
        searchQuery
    ) { games, query ->
        filterGames(games, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArchivedGames: StateFlow<List<Game>> = combine(
        allArchivedGames,
        searchQuery
    ) { games, query ->
        filterGames(games, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filterGames(games: List<Game>, query: String): List<Game> {
        if (query.isBlank()) return games
        return games.filter { game ->
            game.title.contains(query, ignoreCase = true) ||
                    game.notes.contains(query, ignoreCase = true)
        }
    }

    /**
     * Start/Resume timer for a game and display ongoing push notification in the notification panel.
     */
    fun playGame(game: Game, context: android.content.Context = getApplication()) {
        GameTimerSessionManager.startGame(context, game, repository)
    }

    /**
     * Pause the currently running game and dismiss the push notification.
     */
    fun pauseGame(context: android.content.Context = getApplication()) {
        GameTimerSessionManager.pauseGame(context, repository)
    }

    /**
     * Toggle play/pause for a given game.
     */
    fun togglePlayPause(game: Game) {
        if (runningGameId.value == game.id) {
            pauseGame()
        } else {
            playGame(game)
        }
    }

    /**
     * Reordering methods for changing game order (e.g. on long press)
     */
    fun moveGameToTop(game: Game) {
        viewModelScope.launch {
            val currentList = (if (game.isArchived) allArchivedGames.value else allActiveGames.value).toMutableList()
            val currentIndex = currentList.indexOfFirst { it.id == game.id }
            if (currentIndex > 0) {
                currentList.removeAt(currentIndex)
                currentList.add(0, game)
                persistReorderedList(currentList)
            }
        }
    }

    fun moveGameUp(game: Game) {
        viewModelScope.launch {
            val currentList = (if (game.isArchived) allArchivedGames.value else allActiveGames.value).toMutableList()
            val currentIndex = currentList.indexOfFirst { it.id == game.id }
            if (currentIndex > 0) {
                val item = currentList.removeAt(currentIndex)
                currentList.add(currentIndex - 1, item)
                persistReorderedList(currentList)
            }
        }
    }

    fun moveGameDown(game: Game) {
        viewModelScope.launch {
            val currentList = (if (game.isArchived) allArchivedGames.value else allActiveGames.value).toMutableList()
            val currentIndex = currentList.indexOfFirst { it.id == game.id }
            if (currentIndex in 0 until currentList.size - 1) {
                val item = currentList.removeAt(currentIndex)
                currentList.add(currentIndex + 1, item)
                persistReorderedList(currentList)
            }
        }
    }

    fun moveGameToBottom(game: Game) {
        viewModelScope.launch {
            val currentList = (if (game.isArchived) allArchivedGames.value else allActiveGames.value).toMutableList()
            val currentIndex = currentList.indexOfFirst { it.id == game.id }
            if (currentIndex in 0 until currentList.size - 1) {
                val item = currentList.removeAt(currentIndex)
                currentList.add(item)
                persistReorderedList(currentList)
            }
        }
    }

    fun reorderGames(reorderedList: List<Game>) {
        viewModelScope.launch {
            persistReorderedList(reorderedList)
        }
    }

    private suspend fun persistReorderedList(list: List<Game>) {
        val updatedGames = list.mapIndexed { index, game ->
            game.copy(sortOrder = index)
        }
        repository.updateAll(updatedGames)
    }

    /**
     * Manually adjust playtime for a game.
     */
    fun adjustPlaytime(gameId: Long, newTotalSeconds: Long) {
        viewModelScope.launch {
            if (runningGameId.value == gameId) {
                pauseGame()
            }
            repository.updatePlaytime(gameId, newTotalSeconds.coerceAtLeast(0L))
        }
    }

    /**
     * Update freeform notes for a game.
     */
    fun updateGameNotes(gameId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(gameId, notes.trim())
        }
    }

    /**
     * Move game to Archived tab.
     */
    fun archiveGame(gameId: Long) {
        viewModelScope.launch {
            if (runningGameId.value == gameId) {
                pauseGame()
            }
            repository.setArchivedStatus(gameId, true)
        }
    }

    /**
     * Restore game from Archived tab to Active tab.
     */
    fun restoreGame(gameId: Long) {
        viewModelScope.launch {
            repository.setArchivedStatus(gameId, false)
        }
    }

    /**
     * Permanently delete a game.
     */
    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            if (runningGameId.value == gameId) {
                pauseGame()
            }
            repository.deleteGameById(gameId)
        }
    }

    /**
     * Add a new game to track.
     */
    fun addNewGame(
        title: String,
        initialHours: Long,
        initialMinutes: Long,
        initialSeconds: Long,
        colorHex: Long,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val totalSeconds = (initialHours * 3600L) + (initialMinutes * 60L) + initialSeconds
            val currentList = allActiveGames.value
            val maxSortOrder = currentList.maxOfOrNull { it.sortOrder } ?: -1
            val newGame = Game(
                title = title.trim(),
                platform = "",
                genre = "",
                totalPlaytimeSeconds = totalSeconds.coerceAtLeast(0L),
                isArchived = false,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis(),
                lastPlayedAt = null,
                notes = notes.trim(),
                sortOrder = maxSortOrder + 1
            )
            repository.insertGame(newGame)
        }
    }

    /**
     * Export all games to a JSON formatted string.
     */
    suspend fun exportDataAsJson(): String {
        if (runningGameId.value != null) {
            pauseGame()
        }

        val allGames = repository.getAllGamesDirect()
        val root = JSONObject()
        root.put("version", 2)
        root.put("app", "PlayTime")
        root.put("exportedAt", System.currentTimeMillis())

        val gamesArray = JSONArray()
        for (game in allGames) {
            val obj = JSONObject()
            obj.put("title", game.title)
            obj.put("totalPlaytimeSeconds", game.totalPlaytimeSeconds)
            obj.put("isArchived", game.isArchived)
            obj.put("colorHex", game.colorHex)
            obj.put("createdAt", game.createdAt)
            if (game.lastPlayedAt != null) {
                obj.put("lastPlayedAt", game.lastPlayedAt)
            }
            obj.put("notes", game.notes)
            obj.put("sortOrder", game.sortOrder)
            gamesArray.put(obj)
        }
        root.put("games", gamesArray)

        return root.toString(2)
    }

    /**
     * Parse backup JSON string and return summary for confirmation.
     */
    fun parseBackupSummary(jsonString: String): Result<BackupSummary> {
        return try {
            val root = JSONObject(jsonString)
            val gamesArray = root.getJSONArray("games")
            val gamesList = mutableListOf<Game>()
            var totalSec = 0L

            for (i in 0 until gamesArray.length()) {
                val obj = gamesArray.getJSONObject(i)
                val title = obj.optString("title", "Untitled Game")
                val totalPlaytime = obj.optLong("totalPlaytimeSeconds", 0L)
                val isArchived = obj.optBoolean("isArchived", false)
                val colorHex = obj.optLong("colorHex", 0xFF00E5FF)
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val lastPlayedAt = if (obj.has("lastPlayedAt")) obj.optLong("lastPlayedAt") else null
                val notes = obj.optString("notes", "")
                val sortOrder = obj.optInt("sortOrder", i)

                totalSec += totalPlaytime
                gamesList.add(
                    Game(
                        id = 0,
                        title = title,
                        platform = "",
                        genre = "",
                        totalPlaytimeSeconds = totalPlaytime,
                        isArchived = isArchived,
                        colorHex = colorHex,
                        createdAt = createdAt,
                        lastPlayedAt = lastPlayedAt,
                        notes = notes,
                        sortOrder = sortOrder
                    )
                )
            }

            val totalHours = totalSec / 3600
            val totalMins = (totalSec % 3600) / 60
            val formattedPlaytime = "${totalHours}h ${totalMins}m"

            val activeCount = gamesList.count { !it.isArchived }
            val archivedCount = gamesList.count { it.isArchived }

            Result.success(
                BackupSummary(
                    totalCount = gamesList.size,
                    activeCount = activeCount,
                    archivedCount = archivedCount,
                    totalPlaytimeFormatted = formattedPlaytime,
                    games = gamesList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import parsed games into database.
     */
    fun importGames(games: List<Game>, replaceExisting: Boolean, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            if (runningGameId.value != null) {
                pauseGame()
            }
            repository.importGames(games, replaceExisting)
            onComplete(games.size)
        }
    }
}
