package com.sam.audioroutine.feature.player.music

import org.junit.Assert.assertEquals
import org.junit.Test

class DuckingMusicPromptPolicyTest {

    private val policy = DuckingMusicPromptPolicy()

    @Test
    fun onPromptStart_ducksCurrentVolume() {
        assertEquals(0.35f, policy.onPromptStart(1f), 0.0001f)
        assertEquals(0.175f, policy.onPromptStart(0.5f), 0.0001f)
    }

    @Test
    fun onPromptStart_clampsInputVolumeToRange() {
        assertEquals(0f, policy.onPromptStart(-1f), 0.0001f)
        assertEquals(0.35f, policy.onPromptStart(2f), 0.0001f)
    }

    @Test
    fun onPromptEnd_restoresTargetVolume() {
        assertEquals(1f, policy.onPromptEnd(0.2f), 0.0001f)
        assertEquals(1f, policy.onPromptEnd(0.9f), 0.0001f)
    }
}
