package com.sam.audioroutine.feature.player

import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.model.RoutineBlock
import com.sam.audioroutine.domain.repo.RoutineRepository
import java.time.Duration
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun latestRoutine_updatesWhenRepositoryFlowEmits() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = PlayerViewModel(repository, PlaybackProgressBus())

        repository.emitLatest(
            Routine(
                id = 42,
                name = "Updated Routine",
                blocks = listOf(
                    RoutineBlock(
                        id = 7,
                        routineId = 42,
                        position = 0,
                        textToSpeak = "Breathe",
                        waitDuration = Duration.ofMinutes(2),
                        musicStyle = "Focus"
                    )
                )
            )
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Updated Routine", viewModel.uiState.value.latestRoutine?.name)
        assertEquals(1, viewModel.uiState.value.latestRoutine?.blocks?.size)
    }

    @Test
    fun playbackProgress_updatesFromProgressBus() = runTest {
        val progressBus = PlaybackProgressBus()
        val viewModel = PlayerViewModel(FakeRoutineRepository(), progressBus)

        progressBus.update(
            PlaybackProgress(
                isRunning = true,
                routineName = "Routine",
                currentLine = "Move",
                currentBlockIndex = 1,
                totalBlocks = 3,
                currentBlockDurationMillis = 180000,
                currentBlockRemainingMillis = 60000,
                routineDurationMillis = 600000,
                routineRemainingMillis = 420000,
                projectedFinishEpochMillis = 1234567890,
                upcomingActivities = listOf(
                    PlaybackActivitySummary(index = 1, line = "Move", plannedDurationMillis = 180000),
                    PlaybackActivitySummary(index = 2, line = "Cool down", plannedDurationMillis = 120000)
                )
            )
        )

        advanceUntilIdle()

        assertEquals("Move", viewModel.uiState.value.playbackProgress.currentLine)
        assertEquals(1, viewModel.uiState.value.playbackProgress.currentBlockIndex)
        assertEquals(3, viewModel.uiState.value.playbackProgress.totalBlocks)
        assertEquals(60000, viewModel.uiState.value.playbackProgress.currentBlockRemainingMillis)
        assertEquals(420000, viewModel.uiState.value.playbackProgress.routineRemainingMillis)
        assertEquals(2, viewModel.uiState.value.playbackProgress.upcomingActivities.size)
    }
}

private class FakeRoutineRepository : RoutineRepository {
    private val latestRoutineFlow = MutableStateFlow<Routine?>(null)

    override fun observeRoutines(): Flow<List<Routine>> = flowOf(emptyList())

    override fun observeLatestRoutine(): Flow<Routine?> = latestRoutineFlow

    override suspend fun createRoutine(name: String): Long = 1

    override suspend fun saveRoutine(routine: Routine) {
        latestRoutineFlow.value = routine
    }

    override suspend fun deleteRoutine(routineId: Long) {
        if (latestRoutineFlow.value?.id == routineId) {
            latestRoutineFlow.value = null
        }
    }

    override suspend fun getLatestRoutine(): Routine? = latestRoutineFlow.value

    override suspend fun getRoutine(routineId: Long): Routine? = latestRoutineFlow.value

    fun emitLatest(routine: Routine?) {
        latestRoutineFlow.value = routine
    }
}
