package com.sam.audioroutine.domain.repo

import com.sam.audioroutine.domain.model.Routine

interface RoutineSeedSource {
    suspend fun loadBundledRoutine(): Routine?
}
