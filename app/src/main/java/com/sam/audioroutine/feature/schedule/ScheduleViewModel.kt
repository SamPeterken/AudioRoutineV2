package com.sam.audioroutine.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.repo.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimeInputMode {
    START,
    END
}

data class AlarmUiState(
    val alarmId: Long,
    val selectedRoutineId: Long? = null,
    val startTime: LocalTime = LocalTime.of(7, 0),
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val isEnabled: Boolean = false,
    val isExpanded: Boolean = false,
    val timeInputMode: TimeInputMode = TimeInputMode.START,
    val nextTrigger: ZonedDateTime? = null,
    val statusText: String = ""
)

data class ScheduleUiState(
    val routines: List<Routine> = emptyList(),
    val alarms: List<AlarmUiState> = listOf(AlarmUiState(alarmId = 1L)),
    val nextAlarmId: Long = 2L,
    val isLoading: Boolean = true,
    val showAlarmPermissionsPrompt: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val routineScheduler: AlarmScheduler,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        observeRoutines()
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            routineRepository.observeRoutines().collect { routines ->
                val hydratedRoutines = routines.map { routine ->
                    routineRepository.getRoutine(routine.id) ?: routine
                }
                _uiState.update { state ->
                    val defaultRoutineId = hydratedRoutines.lastOrNull()?.id
                    val normalizedAlarms = state.alarms.map { alarm ->
                        val selectedId = alarm.selectedRoutineId
                            ?.takeIf { selected -> hydratedRoutines.any { it.id == selected } }
                            ?: defaultRoutineId
                        alarm.copy(
                            selectedRoutineId = selectedId,
                            statusText = if (hydratedRoutines.isEmpty()) "Create and save a routine first." else alarm.statusText
                        )
                    }
                    state.copy(
                        routines = hydratedRoutines,
                        alarms = normalizedAlarms,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addAlarm() {
        _uiState.update { state ->
            state.copy(
                alarms = state.alarms + AlarmUiState(
                    alarmId = state.nextAlarmId,
                    selectedRoutineId = state.routines.lastOrNull()?.id,
                    isExpanded = true
                ),
                nextAlarmId = state.nextAlarmId + 1
            )
        }
    }

    fun removeAlarm(alarmId: Long) {
        val alarm = _uiState.value.alarms.firstOrNull { it.alarmId == alarmId } ?: return
        if (alarm.isEnabled) {
            routineScheduler.cancel(alarmId)
        }
        _uiState.update { state ->
            state.copy(
                alarms = state.alarms.filterNot { it.alarmId == alarmId }
            )
        }
    }

    fun toggleExpanded(alarmId: Long) {
        updateAlarm(alarmId) { it.copy(isExpanded = !it.isExpanded) }
    }

    fun setEnabled(alarmId: Long, enabled: Boolean) {
        if (!enabled) {
            routineScheduler.cancel(alarmId)
            updateAlarm(alarmId) { alarm ->
                alarm.copy(
                    isEnabled = false,
                    nextTrigger = null,
                    statusText = "Alarm is off"
                )
            }
            return
        }
        scheduleOrUpdateEnabledAlarm(alarmId)
    }

    fun setSelectedRoutine(alarmId: Long, routineId: Long) {
        val currentAlarm = _uiState.value.alarms.firstOrNull { it.alarmId == alarmId } ?: return
        val previousEndTime = computedEndTime(currentAlarm)
        updateAlarm(alarmId) { it.copy(selectedRoutineId = routineId) }
        updateAlarm(alarmId) { alarm ->
            if (alarm.timeInputMode != TimeInputMode.END) {
                alarm
            } else {
                val nextDuration = currentRoutineDuration(alarm)
                alarm.copy(startTime = previousEndTime.minusSeconds(nextDuration.seconds))
            }
        }
        if (_uiState.value.alarms.firstOrNull { it.alarmId == alarmId }?.isEnabled == true) {
            scheduleOrUpdateEnabledAlarm(alarmId)
        }
    }

    fun toggleDay(alarmId: Long, dayOfWeek: DayOfWeek) {
        updateAlarm(alarmId) { alarm ->
            val nextDays = alarm.selectedDays.toMutableSet().apply {
                if (contains(dayOfWeek)) remove(dayOfWeek) else add(dayOfWeek)
            }
            alarm.copy(selectedDays = nextDays)
        }
        if (_uiState.value.alarms.firstOrNull { it.alarmId == alarmId }?.isEnabled == true) {
            scheduleOrUpdateEnabledAlarm(alarmId)
        }
    }

    fun setTimeInputMode(alarmId: Long, mode: TimeInputMode) {
        updateAlarm(alarmId) { it.copy(timeInputMode = mode) }
    }

    fun updateTimeText(alarmId: Long, value: String) {
        val parsedTime = parseTime(value) ?: return
        val mode = _uiState.value.alarms.firstOrNull { it.alarmId == alarmId }?.timeInputMode ?: TimeInputMode.START
        updateTime(alarmId, mode, parsedTime)
    }

    fun updateTime(alarmId: Long, mode: TimeInputMode, time: LocalTime) {
        updateAlarm(alarmId) { alarm ->
            val duration = currentRoutineDuration(alarm)
            val updatedStart = when (mode) {
                TimeInputMode.START -> time
                TimeInputMode.END -> time.minusSeconds(duration.seconds)
            }
            alarm.copy(startTime = updatedStart)
        }
        if (_uiState.value.alarms.firstOrNull { it.alarmId == alarmId }?.isEnabled == true) {
            scheduleOrUpdateEnabledAlarm(alarmId)
        }
    }

    fun dismissAlarmPermissionsPrompt() {
        _uiState.update { it.copy(showAlarmPermissionsPrompt = false) }
    }

    private fun scheduleOrUpdateEnabledAlarm(alarmId: Long) {
        val state = _uiState.value
        val alarm = state.alarms.firstOrNull { it.alarmId == alarmId } ?: return
        val routine = selectedRoutine(alarm)
        if (routine == null) {
            updateAlarm(alarmId) {
                it.copy(
                    isEnabled = false,
                    nextTrigger = null,
                    statusText = "Create and save a routine first."
                )
            }
            return
        }

        if (!routineScheduler.canScheduleExactAlarms()) {
            _uiState.update {
                it.copy(
                    showAlarmPermissionsPrompt = true
                )
            }
            updateAlarm(alarmId) { it.copy(statusText = "Alarm permissions are required.") }
            return
        }

        val request = buildScheduleRequest(alarm = alarm, routineId = routine.id)
        if (request == null) {
            updateAlarm(alarmId) {
                it.copy(
                    isEnabled = false,
                    nextTrigger = null,
                    statusText = "Choose at least one day or use quick next-day alarm."
                )
            }
            return
        }

        val result = routineScheduler.scheduleNext(request)
        if (result.scheduled) {
            updateAlarm(alarmId) {
                it.copy(
                    isEnabled = true,
                    nextTrigger = result.nextTrigger,
                    statusText = "Alarm set"
                )
            }
            _uiState.update { it.copy(showAlarmPermissionsPrompt = false) }
            return
        }

        updateAlarm(alarmId) {
            it.copy(
                isEnabled = false,
                nextTrigger = null,
                statusText = "Could not schedule alarm. Check alarm permissions."
            )
        }
        _uiState.update { it.copy(showAlarmPermissionsPrompt = true) }
    }

    private fun buildScheduleRequest(
        alarm: AlarmUiState,
        routineId: Long,
    ): AlarmScheduleRequest? {
        return if (alarm.selectedDays.isEmpty()) {
            AlarmScheduleRequest(
                alarmId = alarm.alarmId,
                routineId = routineId,
                startTime = alarm.startTime,
                oneShotDate = LocalDate.now(clock).plusDays(1)
            )
        } else {
            AlarmScheduleRequest(
                alarmId = alarm.alarmId,
                routineId = routineId,
                startTime = alarm.startTime,
                daysOfWeek = alarm.selectedDays
            )
        }
    }

    fun selectedRoutine(alarm: AlarmUiState): Routine? {
        val selectedId = alarm.selectedRoutineId ?: return null
        return _uiState.value.routines.firstOrNull { it.id == selectedId }
    }

    fun currentRoutineDuration(alarm: AlarmUiState): Duration {
        return selectedRoutine(alarm)
            ?.blocks
            ?.fold(Duration.ZERO) { acc, block -> acc.plus(block.waitDuration) }
            ?: Duration.ZERO
    }

    fun computedEndTime(alarm: AlarmUiState): LocalTime {
        return alarm.startTime.plusSeconds(currentRoutineDuration(alarm).seconds)
    }

    fun parseTime(value: String): LocalTime? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    private fun updateAlarm(alarmId: Long, transform: (AlarmUiState) -> AlarmUiState) {
        _uiState.update { state ->
            state.copy(
                alarms = state.alarms.map { alarm ->
                    if (alarm.alarmId == alarmId) transform(alarm) else alarm
                }
            )
        }
    }
}
