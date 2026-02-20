package com.sam.audioroutine.feature.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sam.audioroutine.feature.player.PlaybackServiceContract
import java.time.DayOfWeek
import java.time.LocalDate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_START_HOUR = "extra_start_hour"
        const val EXTRA_START_MINUTE = "extra_start_minute"
        const val EXTRA_DAYS_OF_WEEK = "extra_days_of_week"
    }

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    override fun scheduleNext(request: AlarmScheduleRequest): AlarmScheduleResult {
        val now = ZonedDateTime.now()
        val nextTrigger = if (request.oneShotDate != null) {
            request.oneShotDate
                .atTime(request.startTime)
                .atZone(now.zone)
                .withSecond(0)
                .withNano(0)
                .takeIf { it.isAfter(now) }
        } else {
            calculateNextTrigger(
                now = now,
                startTime = request.startTime,
                daysOfWeek = request.daysOfWeek
            )
        } ?: return AlarmScheduleResult(scheduled = false)

        val isScheduled = runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTrigger.toInstant().toEpochMilli(),
                pendingIntentFor(request)
            )
        }.isSuccess
        return AlarmScheduleResult(
            scheduled = isScheduled,
            nextTrigger = if (isScheduled) nextTrigger else null
        )
    }

    override fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntentFor(alarmId))
    }

    private fun calculateNextTrigger(
        now: ZonedDateTime,
        startTime: java.time.LocalTime,
        daysOfWeek: Set<DayOfWeek>
    ): ZonedDateTime? {
        if (daysOfWeek.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek !in daysOfWeek) continue
            val candidate = date
                .atTime(startTime)
                .atZone(now.zone)
                .withSecond(0)
                .withNano(0)
            if (candidate.isAfter(now)) {
                return candidate
            }
        }
        return null
    }

    private fun pendingIntentFor(request: AlarmScheduleRequest): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, request.alarmId)
            putExtra(PlaybackServiceContract.EXTRA_ROUTINE_ID, request.routineId)
            putExtra(EXTRA_START_HOUR, request.startTime.hour)
            putExtra(EXTRA_START_MINUTE, request.startTime.minute)
            putExtra(
                EXTRA_DAYS_OF_WEEK,
                request.daysOfWeek.map { it.getValue() }.sorted().joinToString(",")
            )
        }
        return PendingIntent.getBroadcast(
            context,
            request.alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntentFor(alarmId: Long): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
