package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeCatalogMusicProviderTest {

    private val provider = FreeCatalogMusicProvider()

    @Test
    fun resolve_trackSelectionById_returnsSpecificLibraryTrack() {
        val selectedTrack = FreeCatalogLibrary.findTrackById("soundhelix-2-focus-lofi")
        requireNotNull(selectedTrack)

        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = selectedTrack.id,
                displayName = selectedTrack.title
            )
        )

        assertNotNull(result)
        assertTrue(result!!.isPlayable)
        assertEquals(selectedTrack.uri, result.mediaUri)
        assertEquals(selectedTrack.title, result.displayName)
    }

    @Test
    fun resolve_styleSelection_recommendsPlayableTrack() {
        val recommended = FreeCatalogLibrary.recommend(
            theme = FreeCatalogTheme.FOCUS,
            style = FreeCatalogStyle.LOFI,
            energy = 3,
            limit = 1
        ).first()

        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.STYLE,
                displayName = "Focus lofi medium"
            )
        )

        assertNotNull(result)
        assertTrue(result!!.isPlayable)
        assertEquals(recommended.uri, result.mediaUri)
    }

    @Test
    fun resolve_trackSelectionMissingId_fallsBackToRecommendation() {
        val result = provider.resolve(
            MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = "missing-id",
                displayName = "Calm ambient"
            )
        )

        assertNotNull(result)
        assertTrue(result!!.isPlayable)
        assertNotNull(result.mediaUri)
    }
}
