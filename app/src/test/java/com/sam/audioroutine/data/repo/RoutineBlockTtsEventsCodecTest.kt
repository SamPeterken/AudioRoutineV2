package com.sam.audioroutine.data.repo

import com.sam.audioroutine.domain.model.RecordedPrompt
import com.sam.audioroutine.domain.model.RoutineBlockTtsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineBlockTtsEventsCodecTest {

    @Test
    fun encodeThenDecode_roundTripsValidEvents() {
        val events = listOf(
            RoutineBlockTtsEvent(offsetSeconds = 0L, text = "Start"),
            RoutineBlockTtsEvent(offsetSeconds = 90L, text = "1 minute left")
        )

        val encoded = RoutineBlockTtsEventsCodec.encode(events)
        val decoded = RoutineBlockTtsEventsCodec.decode(encoded)

        assertEquals(events, decoded)
    }

    @Test
    fun decode_returnsEmptyListForInvalidJson() {
        val decoded = RoutineBlockTtsEventsCodec.decode("{not-valid")

        assertTrue(decoded.isEmpty())
    }

    @Test
    fun encodeThenDecode_roundTripsRecordedPromptMetadata() {
        val events = listOf(
            RoutineBlockTtsEvent(
                offsetSeconds = 12L,
                text = "",
                recordedPrompt = RecordedPrompt(
                    filePath = "/tmp/recorded-event.m4a",
                    durationMillis = 2_300L
                )
            )
        )

        val encoded = RoutineBlockTtsEventsCodec.encode(events)
        val decoded = RoutineBlockTtsEventsCodec.decode(encoded)

        assertEquals(1, decoded.size)
        assertEquals("/tmp/recorded-event.m4a", decoded.first().recordedPrompt?.filePath)
        assertEquals(2_300L, decoded.first().recordedPrompt?.durationMillis)
    }
}
