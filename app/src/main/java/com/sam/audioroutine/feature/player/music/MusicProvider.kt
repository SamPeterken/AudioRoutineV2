package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSourceType

interface MusicProvider {
    val source: MusicSourceType
    fun resolve(selection: MusicSelection): ResolvedMusic?
}
