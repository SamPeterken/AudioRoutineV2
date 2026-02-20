package com.sam.audioroutine.feature.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // TODO: rehydrate and reschedule alarms from Room.
    }
}
