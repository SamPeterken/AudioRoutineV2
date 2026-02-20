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
}