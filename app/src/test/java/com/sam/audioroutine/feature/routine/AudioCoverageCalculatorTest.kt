package com.sam.audioroutine.feature.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCoverageCalculatorTest {

    @Test
    fun calculateAudioCoverage_returnsNull_whenInputInvalid() {
        assertNull(calculateAudioCoverage(totalAudioSeconds = 0L, blockSeconds = 60L))
        assertNull(calculateAudioCoverage(totalAudioSeconds = 120L, blockSeconds = 0L))
    }

    @Test
    fun calculateAudioCoverage_reportsRepeat_whenAudioShorterThanBlock() {
        val result = calculateAudioCoverage(totalAudioSeconds = 90L, blockSeconds = 300L)

        assertNotNull(result)
        assertTrue(result!!.repeats)
        assertEquals(0.3f, result.progressFraction)
        assertEquals(90L, result.totalAudioSeconds)
        assertEquals(300L, result.blockSeconds)
    }

    @Test
    fun calculateAudioCoverage_clampsProgress_whenAudioAtOrAboveBlockLength() {
        val sameLength = calculateAudioCoverage(totalAudioSeconds = 300L, blockSeconds = 300L)
        val longer = calculateAudioCoverage(totalAudioSeconds = 500L, blockSeconds = 300L)

        assertNotNull(sameLength)
        assertNotNull(longer)
        assertFalse(sameLength!!.repeats)
        assertFalse(longer!!.repeats)
        assertEquals(1.0f, sameLength.progressFraction)
        assertEquals(1.0f, longer.progressFraction)
    }
}