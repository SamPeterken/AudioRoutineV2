package com.sam.audioroutine.feature.background

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.audioroutine.domain.repo.AppBackgroundRepository
import com.sam.audioroutine.domain.repo.ForegroundTextMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppBackgroundUiState(
    val backgroundUri: String? = null,
    val foregroundTextMode: ForegroundTextMode = ForegroundTextMode.BLACK
)

@HiltViewModel
class AppBackgroundViewModel @Inject constructor(
    private val appBackgroundRepository: AppBackgroundRepository
) : ViewModel() {

    val uiState: StateFlow<AppBackgroundUiState> =
        appBackgroundRepository.observeBackgroundSettings()
            .map { settings ->
                AppBackgroundUiState(
                    backgroundUri = settings.backgroundUri,
                    foregroundTextMode = settings.foregroundTextMode
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppBackgroundUiState()
            )

    fun setBackgroundUri(uri: String) {
        viewModelScope.launch {
            appBackgroundRepository.setBackgroundUri(uri)
        }
    }

    fun clearBackgroundUri() {
        viewModelScope.launch {
            appBackgroundRepository.setBackgroundUri(null)
        }
    }

    fun setForegroundTextMode(mode: ForegroundTextMode) {
        viewModelScope.launch {
            appBackgroundRepository.setForegroundTextMode(mode)
        }
    }
}
