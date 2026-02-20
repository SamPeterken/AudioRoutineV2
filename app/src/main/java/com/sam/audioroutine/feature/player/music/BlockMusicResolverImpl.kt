package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import com.sam.audioroutine.domain.model.RoutineBlock
import javax.inject.Inject

class BlockMusicResolverImpl @Inject constructor(
    providers: Set<@JvmSuppressWildcards MusicProvider>
) : BlockMusicResolver {

    private val providersBySource: Map<MusicSourceType, MusicProvider> =
        providers.associateBy { it.source }

    override fun resolve(block: RoutineBlock): ResolvedMusic? {
        val selection = block.musicSelection ?: block.musicStyle
            ?.takeIf { it.isNotBlank() }
            ?.let {
                MusicSelection(
                    source = MusicSourceType.FREE_CATALOG,
                    type = MusicSelectionType.STYLE,
                    displayName = it
                )
            }
            ?: return null

        val provider = providersBySource[selection.source] ?: return null
        return provider.resolve(selection)
    }
}
