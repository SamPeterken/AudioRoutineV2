package com.sam.audioroutine.feature.player

import com.sam.audioroutine.domain.model.RoutineBlock
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackProgressCalculatorTest {

    @Test
    fun createSnapshot_populatesDurationsAndUpcomingActivities() {
        val blocks = listOf(
            routineBlock(position = 0, text = "Wake up", minutes = 5),
            routineBlock(position = 1, text = "Stretch", minutes = 10),
            routineBlock(position = 2, text = "Hydrate", minutes = 3),
            routineBlock(position = 3, text = "Plan", minutes = 7),
            routineBlock(position = 4, text = "Start work", minutes = 15)
        )

        val snapshot = PlaybackProgressCalculator.createSnapshot(
            routineName = "Morning Routine",
            orderedBlocks = blocks,
            currentBlockIndex = 0,
            currentLine = "Wake up",
            nowEpochMillis = 60_000L,
            routineStartEpochMillis = 0L
        )

        assertTrue(snapshot.isRunning)
        assertEquals(5, snapshot.totalBlocks)
        assertEquals(300_000L, snapshot.currentBlockDurationMillis)
        assertEquals(240_000L, snapshot.currentBlockRemainingMillis)
        assertEquals(2_400_000L, snapshot.routineDurationMillis)
        assertEquals(2_340_000L, snapshot.routineRemainingMillis)
        assertEquals(5, snapshot.upcomingActivities.size)
    }

    @Test
    fun createSnapshot_calculatesRemainingForMidRoutineBlock() {
        val blocks = listOf(
            routineBlock(position = 0, text = "Wake up", minutes = 5),
            routineBlock(position = 1, text = "Stretch", minutes = 10),
            routineBlock(position = 2, text = "Hydrate", minutes = 3)
        )

        val snapshot = PlaybackProgressCalculator.createSnapshot(
            routineName = "Morning Routine",
            orderedBlocks = blocks,
            currentBlockIndex = 1,
            currentLine = "Stretch",
            nowEpochMillis = 540_000L,
            routineStartEpochMillis = 0L
        )

        assertEquals(600_000L, snapshot.currentBlockDurationMillis)
        assertEquals(360_000L, snapshot.currentBlockRemainingMillis)
        assertEquals(1_080_000L, snapshot.routineDurationMillis)
        assertEquals(540_000L, snapshot.routineRemainingMillis)
    }

    @Test
    fun createSnapshot_appliesAdditionalDurationOverrides() {
        val blocks = listOf(
            routineBlock(position = 0, text = "Wake up", minutes = 5),
            routineBlock(position = 1, text = "Stretch", minutes = 10)
        )

        val snapshot = PlaybackProgressCalculator.createSnapshot(
            routineName = "Morning Routine",
            orderedBlocks = blocks,
            currentBlockIndex = 0,
            currentLine = "Wake up",
            nowEpochMillis = 120_000L,
            routineStartEpochMillis = 0L,
            additionalDurationMillisByIndex = mapOf(0 to 60_000L)
        )

        assertEquals(360_000L, snapshot.currentBlockDurationMillis)
        assertEquals(240_000L, snapshot.currentBlockRemainingMillis)
        assertEquals(960_000L, snapshot.routineDurationMillis)
        assertEquals(840_000L, snapshot.routineRemainingMillis)
    }

    @Test
    fun createSnapshot_appliesElapsedAdjustmentForSkip() {
        val blocks = listOf(
            routineBlock(position = 0, text = "Wake up", minutes = 5),
            routineBlock(position = 1, text = "Stretch", minutes = 10)
        )

        val withoutSkip = PlaybackProgressCalculator.createSnapshot(
            routineName = "Morning Routine",
            orderedBlocks = blocks,
            currentBlockIndex = 1,
            currentLine = "Stretch",
            nowEpochMillis = 60_000L,
            routineStartEpochMillis = 0L
        )

        val withSkip = PlaybackProgressCalculator.createSnapshot(
            routineName = "Morning Routine",
            orderedBlocks = blocks,
            currentBlockIndex = 1,
            currentLine = "Stretch",
            nowEpochMillis = 60_000L,
            routineStartEpochMillis = 0L,
            elapsedAdjustmentMillis = 240_000L
        )

        assertEquals(840_000L, withoutSkip.routineRemainingMillis)
        assertEquals(600_000L, withSkip.routineRemainingMillis)
        assertNotEquals(withoutSkip.projectedFinishEpochMillis, withSkip.projectedFinishEpochMillis)
        assertEquals(
            withoutSkip.projectedFinishEpochMillis - 240_000L,
            withSkip.projectedFinishEpochMillis
        )
    }

    private fun routineBlock(position: Int, text: String, minutes: Long): RoutineBlock =
        RoutineBlock(
            id = position.toLong() + 1,
            routineId = 1,
            position = position,
            textToSpeak = text,
            waitDuration = Duration.ofMinutes(minutes),
            musicStyle = null
        )
}
