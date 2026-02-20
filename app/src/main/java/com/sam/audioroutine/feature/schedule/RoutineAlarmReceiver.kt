package com.sam.audioroutine.feature.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sam.audioroutine.feature.player.PlaybackServiceContract
import com.sam.audioroutine.feature.player.RoutinePlaybackService
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class RoutineAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getLongExtra(RoutineScheduler.EXTRA_ALARM_ID, 0L) ?: 0L
        val routineId = intent?.getLongExtra(PlaybackServiceContract.EXTRA_ROUTINE_ID, 0L) ?: 0L
        val serviceIntent = Intent(context, RoutinePlaybackService::class.java).apply {
            action = PlaybackServiceContract.ACTION_START
            if (routineId > 0L) {
                putExtra(PlaybackServiceContract.EXTRA_ROUTINE_ID, routineId)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        if (routineId > 0L) {
            val startHour = intent?.getIntExtra(RoutineScheduler.EXTRA_START_HOUR, -1) ?: -1
            val startMinute = intent?.getIntExtra(RoutineScheduler.EXTRA_START_MINUTE, -1) ?: -1
            val days = intent
                ?.getStringExtra(RoutineScheduler.EXTRA_DAYS_OF_WEEK)
                .orEmpty()
                .split(",")
                .mapNotNull { token -> token.toIntOrNull()?.let(DayOfWeek::of) }
                .toSet()
            if (startHour in 0..23 && startMinute in 0..59 && days.isNotEmpty()) {
                alarmScheduler.scheduleNext(
                    AlarmScheduleRequest(
                        alarmId = if (alarmId > 0L) alarmId else routineId,
                        routineId = routineId,
                        startTime = LocalTime.of(startHour, startMinute),
                        daysOfWeek = days
                    )
                )
            }
        }
    }
}
