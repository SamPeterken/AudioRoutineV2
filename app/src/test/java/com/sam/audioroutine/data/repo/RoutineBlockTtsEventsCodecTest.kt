package com.sam.audioroutine.data.repo

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
}
