package com.sam.audioroutine.data.repo

import android.content.Context
import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.model.RoutineBundleCodec
import com.sam.audioroutine.domain.repo.RoutineSeedSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AssetRoutineSeedSource @Inject constructor(
    @ApplicationContext private val context: Context
) : RoutineSeedSource {

    override suspend fun loadBundledRoutine(): Routine? {
        val bundledRoutineJson = runCatching {
            context.assets.open(BUNDLED_ROUTINE_ASSET_PATH)
                .bufferedReader()
                .use { reader -> reader.readText() }
        }.getOrNull() ?: return null

        return RoutineBundleCodec.decode(bundledRoutineJson)
    }

    private companion object {
        const val BUNDLED_ROUTINE_ASSET_PATH = "bundled_routine.json"
    }
}
