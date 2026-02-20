package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import javax.inject.Inject

class FreeCatalogMusicProvider @Inject constructor() : MusicProvider {
    override val source: MusicSourceType = MusicSourceType.FREE_CATALOG

    private val defaultTrack: FreeCatalogTrack = FreeCatalogLibrary.allTracks().first()

    override fun resolve(selection: MusicSelection): ResolvedMusic? {
        if (selection.source != source) return null

        val selectedTrack = when (selection.type) {
            MusicSelectionType.TRACK -> findTrackSelection(selection)
            MusicSelectionType.STYLE,
            MusicSelectionType.RADIO,
            MusicSelectionType.PLAYLIST,
            MusicSelectionType.RANDOM_IN_PLAYLIST -> findRecommendedTrack(selection)
        }

        val fallbackName = when (selection.type) {
            MusicSelectionType.STYLE -> selectedTrack.title
            MusicSelectionType.TRACK -> selectedTrack.title
            MusicSelectionType.RADIO -> "Free catalog radio"
            MusicSelectionType.PLAYLIST -> "Free catalog playlist"
            MusicSelectionType.RANDOM_IN_PLAYLIST -> "Free catalog random playlist"
        }

        val mediaUri = selectedTrack.uri.takeIf { it.isNotBlank() }
        return ResolvedMusic(
            source = source,
            displayName = selection.displayName?.takeIf { it.isNotBlank() }
                ?: selection.sourceId?.takeIf { it.isNotBlank() }
                ?: fallbackName,
            mediaUri = mediaUri,
            isPlayable = mediaUri != null
        )
    }

    private fun findTrackSelection(selection: MusicSelection): FreeCatalogTrack {
        return selection.sourceId
            ?.let { FreeCatalogLibrary.findTrackById(it) }
            ?: findRecommendedTrack(selection)
    }

    private fun findRecommendedTrack(selection: MusicSelection): FreeCatalogTrack {
        val query = listOf(selection.displayName, selection.sourceId)
            .joinToString(separator = " ")
            .lowercase()
        val theme = when {
            query.contains("focus") -> FreeCatalogTheme.FOCUS
            query.contains("recover") || query.contains("rest") -> FreeCatalogTheme.RECOVERY
            query.contains("uplift") || query.contains("energy") || query.contains("exercise") -> FreeCatalogTheme.UPLIFT
            query.contains("mindful") || query.contains("meditat") -> FreeCatalogTheme.MINDFUL
            else -> FreeCatalogTheme.CALM
        }
        val style = when {
            query.contains("piano") -> FreeCatalogStyle.PIANO
            query.contains("lofi") || query.contains("lo-fi") -> FreeCatalogStyle.LOFI
            query.contains("nature") -> FreeCatalogStyle.NATURE
            query.contains("electronic") -> FreeCatalogStyle.ELECTRONIC
            else -> FreeCatalogStyle.AMBIENT
        }
        val energy = when {
            query.contains("high") || query.contains("energy") || query.contains("exercise") -> 5
            query.contains("medium") || query.contains("focus") -> 3
            else -> 2
        }
        return FreeCatalogLibrary.recommend(theme, style, energy, limit = 1).firstOrNull() ?: defaultTrack
    }
}
