package com.sam.audioroutine.feature.player.music

interface MusicPromptPolicy {
    fun onPromptStart(currentVolume: Float): Float
    fun onPromptEnd(currentVolume: Float): Float
}
