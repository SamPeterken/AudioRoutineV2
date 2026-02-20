package com.sam.audioroutine.feature.player

import com.sam.audioroutine.domain.model.RoutineBlock

object PlaybackProgressCalculator {

    fun createSnapshot(
        routineName: String,
        orderedBlocks: List<RoutineBlock>,
        currentBlockIndex: Int,
        currentPrompt: String,
        nowEpochMillis: Long,
        routineStartEpochMillis: Long,
        additionalDurationMillisByIndex: Map<Int, Long> = emptyMap(),
        elapsedAdjustmentMillis: Long = 0L
    ): PlaybackProgress {
        if (orderedBlocks.isEmpty() || currentBlockIndex !in orderedBlocks.indices) {
            return PlaybackProgress()
        }

        val blockDurations = orderedBlocks.mapIndexed { index, block ->
            val base = block.waitDuration.toMillis().coerceAtLeast(0L)
            val extra = additionalDurationMillisByIndex[index].orEmptyNonNegative()
            base + extra
        }
        val totalRoutineDuration = blockDurations.sum()
        val elapsedRoutine = (nowEpochMillis - routineStartEpochMillis + elapsedAdjustmentMillis)
            .coerceAtLeast(0L)
        val routineRemaining = (totalRoutineDuration - elapsedRoutine).coerceAtLeast(0L)

        val elapsedBeforeCurrent = blockDurations.take(currentBlockIndex).sum()
        val currentBlockDuration = blockDurations[currentBlockIndex]
        val elapsedInCurrentBlock = (elapsedRoutine - elapsedBeforeCurrent).coerceAtLeast(0L)
        val currentBlockRemaining = (currentBlockDuration - elapsedInCurrentBlock).coerceAtLeast(0L)

        return PlaybackProgress(
            isRunning = true,
            routineName = routineName,
            currentBlockIndex = currentBlockIndex,
            totalBlocks = orderedBlocks.size,
            currentPrompt = currentPrompt,
            currentBlockDurationMillis = currentBlockDuration,
            currentBlockRemainingMillis = currentBlockRemaining,
            routineDurationMillis = totalRoutineDuration,
            routineRemainingMillis = routineRemaining,
            projectedFinishEpochMillis = routineStartEpochMillis + totalRoutineDuration - elapsedAdjustmentMillis,
            upcomingActivities = orderedBlocks.mapIndexed { index, block ->
                PlaybackActivitySummary(
                    index = index,
                    prompt = block.textToSpeak,
                    plannedDurationMillis = blockDurations[index]
                )
            }
        )
    }

    private fun Long?.orEmptyNonNegative(): Long = this?.coerceAtLeast(0L) ?: 0L
}
