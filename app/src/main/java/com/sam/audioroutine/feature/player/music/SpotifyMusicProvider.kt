package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSourceType
import javax.inject.Inject

class SpotifyMusicProvider @Inject constructor() : MusicProvider {
    override val source: MusicSourceType = MusicSourceType.SPOTIFY

    override fun resolve(selection: MusicSelection): ResolvedMusic? {
        if (selection.source != source) return null
        return null
    }
}
