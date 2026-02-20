package com.sam.audioroutine.domain.repo

import kotlinx.coroutines.flow.Flow

enum class ForegroundTextMode {
    BLACK,
    WHITE
}

data class AppBackgroundSettings(
    val backgroundUri: String? = null,
    val foregroundTextMode: ForegroundTextMode = ForegroundTextMode.BLACK
)

interface AppBackgroundRepository {
    fun observeBackgroundSettings(): Flow<AppBackgroundSettings>
    suspend fun setBackgroundUri(uri: String?)
    suspend fun setForegroundTextMode(mode: ForegroundTextMode)
}
