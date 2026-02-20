package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import javax.inject.Inject

class LocalFileMusicProvider @Inject constructor() : MusicProvider {
    override val source: MusicSourceType = MusicSourceType.LOCAL_FILE

    override fun resolve(selection: MusicSelection): ResolvedMusic? {
        if (selection.source != source) return null
        val songs = MusicPlaylistCodec.decode(selection.sourceId)
        val selectedUris = songs.map { it.uri }
        val isRandomOrder = selection.type == MusicSelectionType.RANDOM_IN_PLAYLIST
        val playbackQueue = if (isRandomOrder) {
            selectedUris.shuffled()
        } else {
            selectedUris
        }
        val firstUri = playbackQueue.firstOrNull()
        val fallbackDisplayName = when (songs.size) {
            0 -> "Song list"
            1 -> songs.first().title
            else -> "${songs.size} songs"
        }
        return ResolvedMusic(
            source = source,
            displayName = selection.displayName?.takeIf { it.isNotBlank() } ?: fallbackDisplayName,
            mediaUri = firstUri,
            mediaQueueUris = playbackQueue,
            isPlayable = firstUri != null
        )
    }
}
