package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Game
import com.example.data.GameDatabase
import com.example.data.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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

    // Running Timer State
    private val _runningGameId = MutableStateFlow<Long?>(null)
    val runningGameId: StateFlow<Long?> = _runningGameId.asStateFlow()

    private val _elapsedSessionSeconds = MutableStateFlow(0L)
    val elapsedSessionSeconds: StateFlow<Long> = _elapsedSessionSeconds.asStateFlow()

    private var sessionStartRealtime: Long = 0L
    private var timerJob: Job? = null

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

    // Filtered lists (search by title or notes)
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
     * Start/Resume timer for a game.
     */
    fun playGame(game: Game) {
        viewModelScope.launch {
            val currentRunningId = _runningGameId.value
            if (currentRunningId != null && currentRunningId != game.id) {
                commitRunningSession()
            }

            _runningGameId.value = game.id
            sessionStartRealtime = SystemClock.elapsedRealtime()
            _elapsedSessionSeconds.value = 0L

            timerJob?.cancel()
            timerJob = launch {
                while (isActive) {
                    delay(500)
                    val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartRealtime
                    _elapsedSessionSeconds.value = elapsedMillis / 1000
                }
            }
        }
    }

    /**
     * Pause the currently running game.
     */
    fun pauseGame() {
        viewModelScope.launch {
            commitRunningSession()
        }
    }

    /**
     * Toggle play/pause for a given game.
     */
    fun togglePlayPause(game: Game) {
        if (_runningGameId.value == game.id) {
            pauseGame()
        } else {
            playGame(game)
        }
    }

    private suspend fun commitRunningSession() {
        val runningId = _runningGameId.value ?: return
        timerJob?.cancel()
        timerJob = null

        val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartRealtime
        val sessionSeconds = (elapsedMillis / 1000).coerceAtLeast(0L)

        val game = repository.getGameByIdDirect(runningId)
        if (game != null && sessionSeconds > 0) {
            val updatedTotal = game.totalPlaytimeSeconds + sessionSeconds
            repository.updatePlaytime(runningId, updatedTotal)
        }

        _runningGameId.value = null
        _elapsedSessionSeconds.value = 0L
    }

    /**
     * Manually adjust playtime for a game.
     */
    fun adjustPlaytime(gameId: Long, newTotalSeconds: Long) {
        viewModelScope.launch {
            if (_runningGameId.value == gameId) {
                commitRunningSession()
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
            if (_runningGameId.value == gameId) {
                commitRunningSession()
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
            if (_runningGameId.value == gameId) {
                timerJob?.cancel()
                _runningGameId.value = null
                _elapsedSessionSeconds.value = 0L
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
            val newGame = Game(
                title = title.trim(),
                platform = "",
                genre = "",
                totalPlaytimeSeconds = totalSeconds.coerceAtLeast(0L),
                isArchived = false,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis(),
                lastPlayedAt = null,
                notes = notes.trim()
            )
            repository.insertGame(newGame)
        }
    }

    /**
     * Export all games to a JSON formatted string.
     */
    suspend fun exportDataAsJson(): String {
        // Commit any currently running session first
        if (_runningGameId.value != null) {
            commitRunningSession()
        }

        val allGames = repository.getAllGamesDirect()
        val root = JSONObject()
        root.put("version", 1)
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

                totalSec += totalPlaytime
                gamesList.add(
                    Game(
                        id = 0, // Auto-generate new IDs on import
                        title = title,
                        platform = "",
                        genre = "",
                        totalPlaytimeSeconds = totalPlaytime,
                        isArchived = isArchived,
                        colorHex = colorHex,
                        createdAt = createdAt,
                        lastPlayedAt = lastPlayedAt,
                        notes = notes
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
            if (_runningGameId.value != null) {
                commitRunningSession()
            }
            repository.importGames(games, replaceExisting)
            onComplete(games.size)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
