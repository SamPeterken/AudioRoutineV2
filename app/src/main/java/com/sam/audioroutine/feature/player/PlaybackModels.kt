package com.sam.audioroutine.feature.player

data class PlaybackProgress(
    val isRunning: Boolean = false,
    val routineName: String = "",
    val currentBlockIndex: Int = -1,
    val totalBlocks: Int = 0,
    val currentPrompt: String = "",
    val currentBlockDurationMillis: Long = 0L,
    val currentBlockRemainingMillis: Long = 0L,
    val routineDurationMillis: Long = 0L,
    val routineRemainingMillis: Long = 0L,
    val projectedFinishEpochMillis: Long = 0L,
    val upcomingActivities: List<PlaybackActivitySummary> = emptyList()
)

data class PlaybackActivitySummary(
    val index: Int,
    val prompt: String,
    val plannedDurationMillis: Long
)

object PlaybackServiceContract {
    const val ACTION_START = "com.sam.audioroutine.action.START"
    const val ACTION_STOP = "com.sam.audioroutine.action.STOP"
    const val ACTION_SKIP_CURRENT = "com.sam.audioroutine.action.SKIP_CURRENT"
    const val ACTION_ADD_TIME = "com.sam.audioroutine.action.ADD_TIME"
    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_ADD_MILLIS = "extra_add_millis"
}
