package com.sam.audioroutine.feature.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivePlaybackNavigationTest {

    @Test
    fun shouldAutoExitPlayback_returnsTrue_whenPlaybackJustStoppedAfterRunning() {
        assertTrue(shouldAutoExitPlayback(hasSeenRunningPlayback = true, isRunning = false))
    }

    @Test
    fun shouldAutoExitPlayback_returnsFalse_whenPlaybackIsStillRunning() {
        assertFalse(shouldAutoExitPlayback(hasSeenRunningPlayback = true, isRunning = true))
    }

    @Test
    fun shouldAutoExitPlayback_returnsFalse_whenPlaybackNeverStarted() {
        assertFalse(shouldAutoExitPlayback(hasSeenRunningPlayback = false, isRunning = false))
    }

    @Test
    fun shouldForceActivePlaybackRoute_returnsTrue_whenPlaybackRunningOnNonPlaybackRoute() {
        assertTrue(shouldForceActivePlaybackRoute(isRunning = true, currentRoute = "schedule"))
    }

    @Test
    fun shouldForceActivePlaybackRoute_returnsFalse_whenPlaybackRunningOnPlaybackRoute() {
        assertFalse(shouldForceActivePlaybackRoute(isRunning = true, currentRoute = "player_playback"))
    }

    @Test
    fun shouldForceActivePlaybackRoute_returnsFalse_whenPlaybackNotRunning() {
        assertFalse(shouldForceActivePlaybackRoute(isRunning = false, currentRoute = "schedule"))
    }
}
