package com.sam.audioroutine.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class RoutineBlockTtsEventsTest {

    @Test
    fun allTtsEvents_includesMandatoryStartAndSortsEvents() {
        val block = RoutineBlock(
            position = 0,
            textToSpeak = "Start",
            waitDuration = Duration.ofMinutes(2),
            musicStyle = null,
            additionalTtsEvents = listOf(
                RoutineBlockTtsEvent(offsetSeconds = 90L, text = "30 seconds left"),
                RoutineBlockTtsEvent(offsetSeconds = 30L, text = "Keep going")
            )
        )

        val events = block.allTtsEvents()

        assertEquals(listOf(0L, 30L, 90L), events.map { it.offsetSeconds })
        assertEquals("Start", events.first().text)
    }

    @Test
    fun allTtsEvents_excludesOutOfRangeAdditionalEvents() {
        val block = RoutineBlock(
            position = 0,
            textToSpeak = "Start",
            waitDuration = Duration.ofSeconds(60),
            musicStyle = null,
            additionalTtsEvents = listOf(
                RoutineBlockTtsEvent(offsetSeconds = -1L, text = "Invalid"),
                RoutineBlockTtsEvent(offsetSeconds = 61L, text = "Invalid"),
                RoutineBlockTtsEvent(offsetSeconds = 30L, text = "Valid")
            )
        )

        val events = block.allTtsEvents()

        assertEquals(listOf(0L, 30L), events.map { it.offsetSeconds })
    }

    @Test
    fun allTtsEvents_includesRecordedEventsEvenWithoutText() {
        val block = RoutineBlock(
            position = 0,
            textToSpeak = "",
            recordedPrompt = RecordedPrompt(filePath = "/tmp/start.m4a", durationMillis = 1_000L),
            waitDuration = Duration.ofSeconds(90),
            musicStyle = null,
            additionalTtsEvents = listOf(
                RoutineBlockTtsEvent(
                    offsetSeconds = 45L,
                    text = "",
                    recordedPrompt = RecordedPrompt(filePath = "/tmp/half.m4a", durationMillis = 1_500L)
                )
            )
        )

        val events = block.allTtsEvents()

        assertEquals(listOf(0L, 45L), events.map { it.offsetSeconds })
        assertEquals("/tmp/start.m4a", events[0].recordedPrompt?.filePath)
        assertEquals("/tmp/half.m4a", events[1].recordedPrompt?.filePath)
    }
}