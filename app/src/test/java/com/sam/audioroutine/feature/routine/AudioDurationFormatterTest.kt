package com.sam.audioroutine.feature.routine

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDurationFormatterTest {

    @Test
    fun formatTrackDuration_returnsUnknownWhenNullOrNonPositive() {
        assertEquals("?:??", formatTrackDuration(null))
        assertEquals("?:??", formatTrackDuration(0L))
        assertEquals("?:??", formatTrackDuration(-4L))
    }

    @Test
    fun formatTrackDuration_formatsMinuteSecondAndHourLayouts() {
        assertEquals("0:07", formatTrackDuration(7L))
        assertEquals("4:05", formatTrackDuration(245L))
        assertEquals("1:00:00", formatTrackDuration(3600L))
    }
}