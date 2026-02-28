package com.sam.audioroutine.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

enum class AlarmTimeInputMode {
    START,
    END
}

data class PersistedAlarmState(
    val alarms: List<PersistedAlarm> = listOf(PersistedAlarm(alarmId = 1L)),
    val nextAlarmId: Long = 2L
)

data class PersistedAlarm(
    val alarmId: Long,
    val selectedRoutineId: Long? = null,
    val startTime: LocalTime = LocalTime.of(7, 0),
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val isEnabled: Boolean = false,
    val isExpanded: Boolean = false,
    val timeInputMode: AlarmTimeInputMode = AlarmTimeInputMode.START,
    val nextTrigger: ZonedDateTime? = null,
    val statusText: String = ""
)
