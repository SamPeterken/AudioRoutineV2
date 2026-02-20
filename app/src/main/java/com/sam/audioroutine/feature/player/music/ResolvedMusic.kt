package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSourceType

data class ResolvedMusic(
    val source: MusicSourceType,
    val displayName: String,
    val mediaUri: String? = null,
    val mediaQueueUris: List<String> = mediaUri?.let(::listOf) ?: emptyList(),
    val isPlayable: Boolean
)
