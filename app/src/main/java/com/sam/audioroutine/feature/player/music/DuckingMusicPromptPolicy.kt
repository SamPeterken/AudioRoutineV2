package com.sam.audioroutine.feature.player.music

import javax.inject.Inject

class DuckingMusicPromptPolicy @Inject constructor() : MusicPromptPolicy {
    override fun onPromptStart(currentVolume: Float): Float {
        return currentVolume.coerceIn(0f, 1f) * DUCK_RATIO
    }

    override fun onPromptEnd(currentVolume: Float): Float {
        return RESTORE_VOLUME
    }

    private companion object {
        private const val DUCK_RATIO = 0.35f
        private const val RESTORE_VOLUME = 1f
    }
}
