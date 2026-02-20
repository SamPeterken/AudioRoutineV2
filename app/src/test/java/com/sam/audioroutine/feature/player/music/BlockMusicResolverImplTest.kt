package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import com.sam.audioroutine.domain.model.RoutineBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class BlockMusicResolverImplTest {

    @Test
    fun resolve_usesLegacyMusicStyleFallbackWhenSelectionMissing() {
        val freeProvider = RecordingMusicProvider(
            source = MusicSourceType.FREE_CATALOG,
            resolved = ResolvedMusic(
                source = MusicSourceType.FREE_CATALOG,
                displayName = "Fallback",
                isPlayable = false
            )
        )
        val resolver = BlockMusicResolverImpl(setOf(freeProvider))

        val resolved = resolver.resolve(
            RoutineBlock(
                position = 0,
                textToSpeak = "Prompt",
                waitDuration = Duration.ofMinutes(1),
                musicStyle = "Peaceful awakening",
                musicSelection = null
            )
        )

        assertEquals("Fallback", resolved?.displayName)
        assertEquals(1, freeProvider.selections.size)
        val selection = freeProvider.selections.first()
        assertEquals(MusicSourceType.FREE_CATALOG, selection.source)
        assertEquals(MusicSelectionType.STYLE, selection.type)
        assertEquals("Peaceful awakening", selection.displayName)
    }

    @Test
    fun resolve_routesToProviderMatchingSelectionSource() {
        val freeProvider = RecordingMusicProvider(
            source = MusicSourceType.FREE_CATALOG,
            resolved = ResolvedMusic(
                source = MusicSourceType.FREE_CATALOG,
                displayName = "Free",
                isPlayable = false
            )
        )
        val spotifyProvider = RecordingMusicProvider(
            source = MusicSourceType.SPOTIFY,
            resolved = ResolvedMusic(
                source = MusicSourceType.SPOTIFY,
                displayName = "Spotify",
                isPlayable = false
            )
        )
        val resolver = BlockMusicResolverImpl(setOf(freeProvider, spotifyProvider))

        val resolved = resolver.resolve(
            RoutineBlock(
                position = 0,
                textToSpeak = "Prompt",
                waitDuration = Duration.ofMinutes(1),
                musicStyle = null,
                musicSelection = MusicSelection(
                    source = MusicSourceType.SPOTIFY,
                    type = MusicSelectionType.PLAYLIST,
                    sourceId = "playlist-1",
                    displayName = "My Playlist"
                )
            )
        )

        assertEquals("Spotify", resolved?.displayName)
        assertTrue(freeProvider.selections.isEmpty())
        assertEquals(1, spotifyProvider.selections.size)
    }

    @Test
    fun resolve_returnsNullWhenMatchingProviderMissing() {
        val resolver = BlockMusicResolverImpl(
            setOf(
                RecordingMusicProvider(
                    source = MusicSourceType.FREE_CATALOG,
                    resolved = ResolvedMusic(
                        source = MusicSourceType.FREE_CATALOG,
                        displayName = "Free",
                        isPlayable = false
                    )
                )
            )
        )

        val resolved = resolver.resolve(
            RoutineBlock(
                position = 0,
                textToSpeak = "Prompt",
                waitDuration = Duration.ofMinutes(1),
                musicStyle = null,
                musicSelection = MusicSelection(
                    source = MusicSourceType.SPOTIFY,
                    type = MusicSelectionType.TRACK,
                    sourceId = "track-1",
                    displayName = "Track"
                )
            )
        )

        assertNull(resolved)
    }
}

private class RecordingMusicProvider(
    override val source: MusicSourceType,
    private val resolved: ResolvedMusic?
) : MusicProvider {

    val selections: MutableList<MusicSelection> = mutableListOf()

    override fun resolve(selection: MusicSelection): ResolvedMusic? {
        selections += selection
        return resolved
    }
}
