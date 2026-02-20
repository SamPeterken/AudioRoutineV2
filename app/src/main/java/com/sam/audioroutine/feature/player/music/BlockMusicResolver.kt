package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.RoutineBlock

interface BlockMusicResolver {
    fun resolve(block: RoutineBlock): ResolvedMusic?
}
