package com.example.notification

import android.content.Context
import android.os.SystemClock
import com.example.data.Game
import com.example.data.GameDatabase
import com.example.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object GameTimerSessionManager {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _runningGameId = MutableStateFlow<Long?>(null)
    val runningGameId: StateFlow<Long?> = _runningGameId.asStateFlow()

    private val _runningGame = MutableStateFlow<Game?>(null)
    val runningGame: StateFlow<Game?> = _runningGame.asStateFlow()

    private val _elapsedSessionSeconds = MutableStateFlow(0L)
    val elapsedSessionSeconds: StateFlow<Long> = _elapsedSessionSeconds.asStateFlow()

    private var sessionStartRealtime: Long = 0L
    private var tickerJob: Job? = null

    var onPauseListener: (() -> Unit)? = null

    fun startGame(context: Context, game: Game, repository: GameRepository) {
        val currentRunningId = _runningGameId.value
        if (currentRunningId != null && currentRunningId != game.id) {
            pauseGame(context, repository)
        }

        _runningGameId.value = game.id
        _runningGame.value = game
        sessionStartRealtime = SystemClock.elapsedRealtime()
        _elapsedSessionSeconds.value = 0L

        // Show ongoing push notification in the phone's notification panel
        TimerNotificationManager.showTimerNotification(context, game, sessionStartRealtime)

        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(500)
                val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartRealtime
                _elapsedSessionSeconds.value = elapsedMillis / 1000
            }
        }
    }

    fun pauseGame(context: Context, repository: GameRepository) {
        val runningId = _runningGameId.value ?: return
        tickerJob?.cancel()
        tickerJob = null

        val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartRealtime
        val sessionSeconds = (elapsedMillis / 1000).coerceAtLeast(0L)

        // Dismiss notification
        TimerNotificationManager.cancelTimerNotification(context)

        scope.launch(Dispatchers.IO) {
            val game = repository.getGameByIdDirect(runningId)
            if (game != null && sessionSeconds > 0) {
                val updatedTotal = game.totalPlaytimeSeconds + sessionSeconds
                repository.updatePlaytime(runningId, updatedTotal)
            }
        }

        _runningGameId.value = null
        _runningGame.value = null
        _elapsedSessionSeconds.value = 0L

        onPauseListener?.invoke()
    }

    fun pauseFromBroadcast(context: Context) {
        val runningId = _runningGameId.value ?: return
        tickerJob?.cancel()
        tickerJob = null

        val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartRealtime
        val sessionSeconds = (elapsedMillis / 1000).coerceAtLeast(0L)

        TimerNotificationManager.cancelTimerNotification(context)

        scope.launch(Dispatchers.IO) {
            val db = GameDatabase.getInstance(context, scope)
            val repository = GameRepository(db.gameDao())
            val game = repository.getGameByIdDirect(runningId)
            if (game != null && sessionSeconds > 0) {
                val updatedTotal = game.totalPlaytimeSeconds + sessionSeconds
                repository.updatePlaytime(runningId, updatedTotal)
            }
        }

        _runningGameId.value = null
        _runningGame.value = null
        _elapsedSessionSeconds.value = 0L

        onPauseListener?.invoke()
    }
}
