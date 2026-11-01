package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.Game

object TimerNotificationManager {
    const val CHANNEL_ID = "game_timer_active_channel"
    const val NOTIFICATION_ID = 2001
    const val ACTION_PAUSE_TIMER = "com.example.ACTION_PAUSE_TIMER"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Active Game Timer"
            val descriptionText = "Displays real-time game timer in the notification panel while playing"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
                enableVibration(false)
                enableLights(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerNotification(
        context: Context,
        game: Game,
        sessionStartRealtime: Long
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Open MainActivity when tapping notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Pause Timer directly from notification panel
        val pauseIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = ACTION_PAUSE_TIMER
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsedMillis = (SystemClock.elapsedRealtime() - sessionStartRealtime).coerceAtLeast(0L)
        val chronometerBase = SystemClock.elapsedRealtime() - elapsedMillis

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Playing: ${game.title}")
            .setContentText(
                if (game.notes.isNotBlank()) game.notes
                else "Playtime session in progress • Tap to open"
            )
            .setOngoing(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setWhen(chronometerBase)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "PAUSE",
                pausePendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }

    fun cancelTimerNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }
}
