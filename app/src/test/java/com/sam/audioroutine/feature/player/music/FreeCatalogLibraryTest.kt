package com.sam.audioroutine.feature.player.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeCatalogLibraryTest {

    @Test
    fun allTracks_returnsCuratedCatalog() {
        val tracks = FreeCatalogLibrary.allTracks()

        assertEquals(6, tracks.size)
        assertTrue(tracks.all { it.uri.startsWith("https://") })
    }

    @Test
    fun findTrackById_returnsExactMatchOrNull() {
        val found = FreeCatalogLibrary.findTrackById("soundhelix-1-calm-ambient")
        val missing = FreeCatalogLibrary.findTrackById("does-not-exist")

        assertNotNull(found)
        assertEquals("Morning Drift", found!!.title)
        assertNull(missing)
    }

    @Test
    fun recommend_prefersExactThemeStyleAndEnergyProximity() {
        val recommendations = FreeCatalogLibrary.recommend(
            theme = FreeCatalogTheme.FOCUS,
            style = FreeCatalogStyle.LOFI,
            energy = 3,
            limit = 3
        )

        assertEquals(3, recommendations.size)
        assertEquals("soundhelix-2-focus-lofi", recommendations.first().id)
        assertEquals(FreeCatalogTheme.FOCUS, recommendations.first().theme)
        assertEquals(FreeCatalogStyle.LOFI, recommendations.first().style)
    }
}
