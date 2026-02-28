package com.sam.audioroutine.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class RoutineBundleCodecTest {

    @Test
    fun encodeAndDecode_roundTripsRoutine() {
        val routine = Routine(
            name = "Sam + Jess",
            blocks = listOf(
                RoutineBlock(
                    position = 0,
                    textToSpeak = "Wake up together",
                    waitDuration = Duration.ofMinutes(5),
                    musicStyle = "Warm Piano",
                    musicSelection = MusicSelection(
                        source = MusicSourceType.FREE_CATALOG,
                        type = MusicSelectionType.TRACK,
                        sourceId = "sample-piano-mindful",
                        displayName = "Sample Piano Mindful"
                    ),
                    additionalTtsEvents = listOf(
                        RoutineBlockTtsEvent(offsetSeconds = 120L, text = "Halfway")
                    )
                )
            )
        )

        val encoded = RoutineBundleCodec.encode(routine)
        val decoded = RoutineBundleCodec.decode(encoded)

        assertNotNull(decoded)
        assertEquals("Sam + Jess", decoded?.name)
        assertEquals(1, decoded?.blocks?.size)
        assertEquals("Wake up together", decoded?.blocks?.firstOrNull()?.textToSpeak)
        assertEquals(300L, decoded?.blocks?.firstOrNull()?.waitDuration?.seconds)
    }

    @Test
    fun decode_returnsNullForInvalidPayload() {
        val decoded = RoutineBundleCodec.decode("{\"name\":\"\",\"blocks\":[]}")

        assertNull(decoded)
    }
}
