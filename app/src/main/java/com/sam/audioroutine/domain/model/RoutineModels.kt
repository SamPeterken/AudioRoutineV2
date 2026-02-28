package com.sam.audioroutine.domain.model

import java.time.Duration
import java.time.LocalTime

data class Routine(
    val id: Long = 0,
    val name: String,
    val blocks: List<RoutineBlock>
)

enum class MusicSourceType {
    FREE_CATALOG,
    LOCAL_FILE,
    SPOTIFY
}

enum class MusicSelectionType {
    STYLE,
    TRACK,
    RADIO,
    PLAYLIST,
    RANDOM_IN_PLAYLIST
}

data class MusicSelection(
    val source: MusicSourceType,
    val type: MusicSelectionType,
    val sourceId: String? = null,
    val displayName: String? = null
)

data class RoutineBlockTtsEvent(
    val offsetSeconds: Long,
    val text: String,
    val recordedPrompt: RecordedPrompt? = null
)

data class RecordedPrompt(
    val filePath: String,
    val durationMillis: Long = 0L
)

data class RoutineBlock(
    val id: Long = 0,
    val routineId: Long = 0,
    val position: Int,
    val title: String = "",
    val textToSpeak: String,
    val recordedPrompt: RecordedPrompt? = null,
    val waitDuration: Duration,
    val musicStyle: String?,
    val musicSelection: MusicSelection? = null,
    val additionalTtsEvents: List<RoutineBlockTtsEvent> = emptyList()
)

fun RoutineBlock.allTtsEvents(): List<RoutineBlockTtsEvent> {
    val startEvent = RoutineBlockTtsEvent(
        offsetSeconds = 0L,
        text = textToSpeak,
        recordedPrompt = recordedPrompt
    )
    return (listOf(startEvent) + additionalTtsEvents)
        .filter {
            it.offsetSeconds >= 0L &&
                it.offsetSeconds <= waitDuration.seconds &&
                (it.recordedPrompt != null || it.text.isNotBlank())
        }
        .sortedBy { it.offsetSeconds }
}

data class RoutineSchedule(
    val id: Long = 0,
    val routineId: Long,
    val triggerAt: LocalTime,
    val daysOfWeek: Set<Int> = emptySet(),
    val enabled: Boolean = true
)
