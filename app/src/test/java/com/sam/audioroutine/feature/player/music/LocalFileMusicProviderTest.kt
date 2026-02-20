package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFileMusicProviderTest {

    private val provider = LocalFileMusicProvider()

    @Test
    fun resolve_returnsQueueAndPlayableResultForEncodedUris() {
        val songs = listOf(
            PlaylistSong(
                source = MusicSourceType.LOCAL_FILE,
                title = "Local 1",
                uri = "content://media/external/audio/media/11"
            ),
            PlaylistSong(
                source = MusicSourceType.FREE_CATALOG,
                title = "Built in",
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            ),
            PlaylistSong(
                source = MusicSourceType.LOCAL_FILE,
                title = "Local 2",
                uri = "content://media/external/audio/media/22"
            )
        )
        val sourceId = MusicPlaylistCodec.encode(songs)

        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.LOCAL_FILE,
                type = MusicSelectionType.RANDOM_IN_PLAYLIST,
                sourceId = sourceId,
                displayName = "My local mix"
            )
        )

        assertNotNull(result)
        assertTrue(result!!.isPlayable)
        assertEquals("My local mix", result.displayName)
        assertEquals(songs.map { it.uri }.toSet(), result.mediaQueueUris.toSet())
        assertTrue(result.mediaUri in songs.map { it.uri })
    }

    @Test
    fun resolve_playlistType_keepsEncodedOrder() {
        val orderedUris = listOf(
            "content://media/external/audio/media/1",
            "content://media/external/audio/media/2",
            "content://media/external/audio/media/3"
        )
        val sourceId = MusicPlaylistCodec.encode(
            orderedUris.mapIndexed { index, uri ->
                PlaylistSong(
                    source = MusicSourceType.LOCAL_FILE,
                    title = "Song $index",
                    uri = uri
                )
            }
        )

        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.LOCAL_FILE,
                type = MusicSelectionType.PLAYLIST,
                sourceId = sourceId,
                displayName = "Ordered"
            )
        )

        assertNotNull(result)
        assertEquals(orderedUris, result!!.mediaQueueUris)
    }

    @Test
    fun resolve_supportsLegacySingleUriSourceId() {
        val legacyUri = "content://media/external/audio/media/101"

        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.LOCAL_FILE,
                type = MusicSelectionType.TRACK,
                sourceId = legacyUri,
                displayName = null
            )
        )

        assertNotNull(result)
        assertTrue(result!!.isPlayable)
        assertEquals(legacyUri, result.mediaUri)
        assertEquals(listOf(legacyUri), result.mediaQueueUris)
    }

    @Test
    fun resolve_returnsNullForOtherSources() {
        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = "id",
                displayName = "Name"
            )
        )

        assertNull(result)
    }

    @Test
    fun resolve_returnsNonPlayableWhenSelectionHasNoUris() {
        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.LOCAL_FILE,
                type = MusicSelectionType.PLAYLIST,
                sourceId = null,
                displayName = null
            )
        )

        assertNotNull(result)
        assertFalse(result!!.isPlayable)
        assertTrue(result.mediaQueueUris.isEmpty())
        assertNull(result.mediaUri)
    }
}