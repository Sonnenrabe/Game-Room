package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.Game

object TimerNotificationManager {
    const val CHANNEL_ID = "game_playtime_channel_v5"
    const val NOTIFICATION_ID = 4001
    const val TEST_NOTIFICATION_ID = 4002
    const val ACTION_PAUSE_TIMER = "com.example.ACTION_PAUSE_TIMER"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Active Game Playtime"
            val descriptionText = "Shows real-time timer in notification panel while playing"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
                enableVibration(false)
                enableLights(true)
                lightColor = 0xFF00E5FF.toInt()
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun areNotificationsAllowed(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }

        return true
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handled
        }
    }

    fun buildNotification(
        context: Context,
        game: Game,
        sessionStartRealtime: Long
    ): Notification {
        createNotificationChannel(context)

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

        // CRITICAL: setWhen() expects epoch wall-clock milliseconds (System.currentTimeMillis),
        // NOT SystemClock.elapsedRealtime(). Using wall-clock timestamp ensures chronometer counts
        // up correctly and is not dropped as an expired 1970 notification.
        val elapsedMillis = (SystemClock.elapsedRealtime() - sessionStartRealtime).coerceAtLeast(0L)
        val sessionStartWallTime = System.currentTimeMillis() - elapsedMillis

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setColor(0xFF00E5FF.toInt())
            .setContentTitle("Playing: ${game.title}")
            .setContentText(
                if (game.notes.isNotBlank()) game.notes
                else "Playtime timer is actively running"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setWhen(sessionStartWallTime)
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0, // Passing 0 for action icon avoids RemoteViews VectorDrawable inflation failures in SystemUI
                "PAUSE",
                pausePendingIntent
            )
            .build()
    }

    fun showTimerNotification(
        context: Context,
        game: Game,
        sessionStartRealtime: Long
    ): Boolean {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return try {
            val notification = buildNotification(context, game, sessionStartRealtime)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: Exception) {
            // Fallback: If full notification fails on this device, post direct streamlined notification
            try {
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val elapsedMillis = (SystemClock.elapsedRealtime() - sessionStartRealtime).coerceAtLeast(0L)
                val sessionStartWallTime = System.currentTimeMillis() - elapsedMillis

                val fallbackNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setColor(0xFF00E5FF.toInt())
                    .setContentTitle("Playing: ${game.title}")
                    .setContentText("Timer is actively running")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setUsesChronometer(true)
                    .setWhen(sessionStartWallTime)
                    .setContentIntent(contentPendingIntent)
                    .build()

                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, fallbackNotification)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun sendTestNotification(context: Context): Boolean {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setColor(0xFF00E5FF.toInt())
            .setContentTitle("PlayTime: Notification Test")
            .setContentText("Notifications are working properly on your phone!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelTimerNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
            // Handled safely
        }
    }
}
