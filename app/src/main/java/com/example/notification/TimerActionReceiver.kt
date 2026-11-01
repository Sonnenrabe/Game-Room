package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == TimerNotificationManager.ACTION_PAUSE_TIMER) {
            GameTimerSessionManager.pauseFromBroadcast(context)
        }
    }
}
