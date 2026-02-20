package com.sam.audioroutine.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sam.audioroutine.domain.repo.AppBackgroundRepository
import com.sam.audioroutine.domain.repo.AppBackgroundSettings
import com.sam.audioroutine.domain.repo.ForegroundTextMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backgroundDataStore by preferencesDataStore(name = "background_settings")

class AppBackgroundRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppBackgroundRepository {

    override fun observeBackgroundSettings(): Flow<AppBackgroundSettings> {
        return context.backgroundDataStore.data.map { preferences ->
            AppBackgroundSettings(
                backgroundUri = preferences[BACKGROUND_URI_KEY],
                foregroundTextMode = preferences[FOREGROUND_TEXT_MODE_KEY]
                    ?.let { value ->
                        runCatching { ForegroundTextMode.valueOf(value) }
                            .getOrElse { ForegroundTextMode.BLACK }
                    }
                    ?: ForegroundTextMode.BLACK
            )
        }
    }

    override suspend fun setBackgroundUri(uri: String?) {
        context.backgroundDataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(BACKGROUND_URI_KEY)
            } else {
                preferences[BACKGROUND_URI_KEY] = uri
            }
        }
    }

    override suspend fun setForegroundTextMode(mode: ForegroundTextMode) {
        context.backgroundDataStore.edit { preferences ->
            preferences[FOREGROUND_TEXT_MODE_KEY] = mode.name
        }
    }

    private companion object {
        val BACKGROUND_URI_KEY = stringPreferencesKey("background_uri")
        val FOREGROUND_TEXT_MODE_KEY = stringPreferencesKey("foreground_text_mode")
    }
}
