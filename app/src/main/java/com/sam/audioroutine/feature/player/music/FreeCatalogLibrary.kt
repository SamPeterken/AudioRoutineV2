package com.sam.audioroutine.feature.player.music

enum class FreeCatalogTheme {
    CALM,
    FOCUS,
    RECOVERY,
    UPLIFT,
    MINDFUL
}

enum class FreeCatalogStyle {
    AMBIENT,
    PIANO,
    LOFI,
    NATURE,
    ELECTRONIC
}

data class FreeCatalogTrack(
    val id: String,
    val title: String,
    val uri: String,
    val theme: FreeCatalogTheme,
    val style: FreeCatalogStyle,
    val energy: Int
)

object FreeCatalogLibrary {
    private val tracks: List<FreeCatalogTrack> = listOf(
        FreeCatalogTrack(
            id = "soundhelix-1-calm-ambient",
            title = "Morning Drift",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            theme = FreeCatalogTheme.CALM,
            style = FreeCatalogStyle.AMBIENT,
            energy = 2
        ),
        FreeCatalogTrack(
            id = "soundhelix-2-focus-lofi",
            title = "Focus Current",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            theme = FreeCatalogTheme.FOCUS,
            style = FreeCatalogStyle.LOFI,
            energy = 3
        ),
        FreeCatalogTrack(
            id = "soundhelix-3-recovery-nature",
            title = "Recovery Breeze",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            theme = FreeCatalogTheme.RECOVERY,
            style = FreeCatalogStyle.NATURE,
            energy = 1
        ),
        FreeCatalogTrack(
            id = "jazz-uplift-electronic",
            title = "City Lift",
            uri = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3",
            theme = FreeCatalogTheme.UPLIFT,
            style = FreeCatalogStyle.ELECTRONIC,
            energy = 4
        ),
        FreeCatalogTrack(
            id = "sample-piano-mindful",
            title = "Mindful Keys",
            uri = "https://samplelib.com/lib/preview/mp3/sample-12s.mp3",
            theme = FreeCatalogTheme.MINDFUL,
            style = FreeCatalogStyle.PIANO,
            energy = 1
        ),
        FreeCatalogTrack(
            id = "soundhelix-1-uplift-lofi",
            title = "Momentum Pulse",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            theme = FreeCatalogTheme.UPLIFT,
            style = FreeCatalogStyle.LOFI,
            energy = 5
        )
    )

    fun allTracks(): List<FreeCatalogTrack> = tracks

    fun findTrackById(id: String): FreeCatalogTrack? {
        return tracks.firstOrNull { it.id == id }
    }

    fun recommend(
        theme: FreeCatalogTheme,
        style: FreeCatalogStyle,
        energy: Int,
        limit: Int = 3
    ): List<FreeCatalogTrack> {
        val normalizedEnergy = energy.coerceIn(1, 5)
        val maxCount = limit.coerceAtLeast(1)
        return tracks
            .sortedWith(
                compareByDescending<FreeCatalogTrack> {
                    recommendationScore(it, theme, style, normalizedEnergy)
                }.thenBy { kotlin.math.abs(it.energy - normalizedEnergy) }
                    .thenBy { it.title }
            )
            .take(maxCount)
    }

    private fun recommendationScore(
        track: FreeCatalogTrack,
        theme: FreeCatalogTheme,
        style: FreeCatalogStyle,
        targetEnergy: Int
    ): Int {
        val themeScore = if (track.theme == theme) 8 else 0
        val styleScore = if (track.style == style) 6 else 0
        val energyDistance = kotlin.math.abs(track.energy - targetEnergy)
        val energyScore = (5 - energyDistance).coerceAtLeast(0)
        return themeScore + styleScore + energyScore
    }
}
