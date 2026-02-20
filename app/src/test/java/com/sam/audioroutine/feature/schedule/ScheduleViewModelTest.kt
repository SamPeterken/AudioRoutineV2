package com.sam.audioroutine.feature.schedule

import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.model.RoutineBlock
import com.sam.audioroutine.domain.repo.RoutineRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setEnabled_showsExactAlarmSettingsPrompt_whenExactAlarmPermissionMissing() = runTest {
        val scheduler = FakeAlarmScheduler(canScheduleExactAlarms = false)
        val viewModel = ScheduleViewModel(FakeRoutineRepository(), scheduler)

        advanceUntilIdle()
        val alarmId = viewModel.uiState.value.alarms.first().alarmId
        viewModel.setEnabled(alarmId, true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAlarmPermissionsPrompt)
        assertEquals(0, scheduler.scheduleNextCallCount)
    }

    @Test
    fun setEnabled_withoutWeekdays_schedulesOneShotForNextDay() = runTest {
        val scheduler = FakeAlarmScheduler(canScheduleExactAlarms = true)
        val fixedClock = Clock.fixed(Instant.parse("2026-02-20T08:00:00Z"), ZoneId.of("UTC"))
        val viewModel = ScheduleViewModel(FakeRoutineRepository(), scheduler, fixedClock)

        advanceUntilIdle()
        val alarmId = viewModel.uiState.value.alarms.first().alarmId
        viewModel.setEnabled(alarmId, true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAlarmPermissionsPrompt)
        assertEquals(1, scheduler.scheduleNextCallCount)
        assertEquals(alarmId, scheduler.lastRequest?.alarmId)
        assertEquals(LocalTime.of(7, 0), scheduler.lastRequest?.startTime)
        assertEquals(2026, scheduler.lastRequest?.oneShotDate?.year)
        assertEquals(2, scheduler.lastRequest?.oneShotDate?.monthValue)
        assertEquals(21, scheduler.lastRequest?.oneShotDate?.dayOfMonth)
        assertTrue(scheduler.lastRequest?.daysOfWeek?.isEmpty() == true)
    }

    @Test
    fun updateTimeText_inEndMode_updatesStartTimeFromRoutineDuration() = runTest {
        val scheduler = FakeAlarmScheduler(canScheduleExactAlarms = true)
        val viewModel = ScheduleViewModel(FakeRoutineRepository(), scheduler)

        advanceUntilIdle()
        val alarm = viewModel.uiState.value.alarms.first()
        viewModel.setTimeInputMode(alarm.alarmId, TimeInputMode.END)
        viewModel.updateTimeText(alarm.alarmId, "08:00")

        val updatedAlarm = viewModel.uiState.value.alarms.first()

        assertEquals(LocalTime.of(7, 57), updatedAlarm.startTime)
        assertEquals(LocalTime.of(8, 0), viewModel.computedEndTime(updatedAlarm))
    }

    @Test
    fun toggleDay_thenEnable_schedulesRepeatingWeekdayAlarm() = runTest {
        val scheduler = FakeAlarmScheduler(canScheduleExactAlarms = true)
        val viewModel = ScheduleViewModel(FakeRoutineRepository(), scheduler)

        advanceUntilIdle()
        val alarmId = viewModel.uiState.value.alarms.first().alarmId
        viewModel.toggleDay(alarmId, DayOfWeek.MONDAY)
        viewModel.setEnabled(alarmId, true)
        advanceUntilIdle()

        assertTrue(scheduler.lastRequest?.oneShotDate == null)
        assertEquals(setOf(DayOfWeek.MONDAY), scheduler.lastRequest?.daysOfWeek)
    }

    @Test
    fun addAlarm_createsSecondIndependentAlarm() = runTest {
        val scheduler = FakeAlarmScheduler(canScheduleExactAlarms = true)
        val viewModel = ScheduleViewModel(FakeRoutineRepository(), scheduler)

        advanceUntilIdle()
        viewModel.addAlarm()

        assertEquals(2, viewModel.uiState.value.alarms.size)
        assertEquals(2L, viewModel.uiState.value.alarms.last().alarmId)
    }

    @Test
    fun setSelectedRoutine_inEndMode_keepsEndTimeAndRecomputesStart() = runTest {
        val scheduler = FakeAlarmScheduler(canScheduleExactAlarms = true)
        val viewModel = ScheduleViewModel(FakeRoutineRepository(withMultipleRoutines = true), scheduler)

        advanceUntilIdle()
        val alarm = viewModel.uiState.value.alarms.first()
        viewModel.setTimeInputMode(alarm.alarmId, TimeInputMode.END)
        viewModel.setSelectedRoutine(alarm.alarmId, 1L)
        viewModel.updateTime(alarm.alarmId, TimeInputMode.END, LocalTime.of(8, 0))

        val afterFirst = viewModel.uiState.value.alarms.first()
        val fixedEnd = viewModel.computedEndTime(afterFirst)

        viewModel.setSelectedRoutine(alarm.alarmId, 2L)
        val afterSwitch = viewModel.uiState.value.alarms.first()

        assertEquals(fixedEnd, viewModel.computedEndTime(afterSwitch))
        assertEquals(LocalTime.of(7, 55), afterSwitch.startTime)
    }
}

private class FakeRoutineRepository(
    private val withMultipleRoutines: Boolean = false
) : RoutineRepository {

    private val routine = Routine(
        id = 1,
        name = "Morning Routine",
        blocks = emptyList()
    )

    private val secondRoutine = Routine(
        id = 2,
        name = "Long Routine",
        blocks = emptyList()
    )

    private val hydratedRoutine = routine.copy(
        blocks = listOf(
            RoutineBlock(
                id = 1,
                routineId = 1,
                position = 0,
                textToSpeak = "Start",
                waitDuration = Duration.ofMinutes(1),
                musicStyle = "Calm"
            ),
            RoutineBlock(
                id = 2,
                routineId = 1,
                position = 1,
                textToSpeak = "Middle",
                waitDuration = Duration.ofMinutes(2),
                musicStyle = "Calm"
            )
        )
    )

    private val hydratedSecondRoutine = secondRoutine.copy(
        blocks = listOf(
            RoutineBlock(
                id = 3,
                routineId = 2,
                position = 0,
                textToSpeak = "Long A",
                waitDuration = Duration.ofMinutes(3),
                musicStyle = "Calm"
            ),
            RoutineBlock(
                id = 4,
                routineId = 2,
                position = 1,
                textToSpeak = "Long B",
                waitDuration = Duration.ofMinutes(2),
                musicStyle = "Calm"
            )
        )
    )

    private val routinesFlow = MutableStateFlow(
        if (withMultipleRoutines) listOf(routine, secondRoutine) else listOf(routine)
    )

    override fun observeRoutines(): Flow<List<Routine>> = routinesFlow

    override fun observeLatestRoutine(): Flow<Routine?> = flowOf(routine)

    override suspend fun createRoutine(name: String): Long = routine.id

    override suspend fun saveRoutine(routine: Routine) = Unit

    override suspend fun deleteRoutine(routineId: Long) = Unit

    override suspend fun getLatestRoutine(): Routine? = routinesFlow.value.lastOrNull()

    override suspend fun getRoutine(routineId: Long): Routine? = when (routineId) {
        1L -> hydratedRoutine
        2L -> hydratedSecondRoutine
        else -> null
    }
}

private class FakeAlarmScheduler(
    private val canScheduleExactAlarms: Boolean
) : AlarmScheduler {
    var scheduleNextCallCount: Int = 0
    var lastRequest: AlarmScheduleRequest? = null

    override fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarms

    override fun scheduleNext(request: AlarmScheduleRequest): AlarmScheduleResult {
        scheduleNextCallCount += 1
        lastRequest = request
        return AlarmScheduleResult(scheduled = true)
    }

    override fun cancel(alarmId: Long) = Unit
}
