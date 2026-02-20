package com.sam.audioroutine.feature.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class AlarmScheduleRequest(
    val alarmId: Long,
    val routineId: Long,
    val startTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val oneShotDate: LocalDate? = null
)

data class AlarmScheduleResult(
    val scheduled: Boolean,
    val nextTrigger: ZonedDateTime? = null
)

interface AlarmScheduler {
    fun canScheduleExactAlarms(): Boolean

    fun scheduleNext(request: AlarmScheduleRequest): AlarmScheduleResult

    fun cancel(alarmId: Long)
}