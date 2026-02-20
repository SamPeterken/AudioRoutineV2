package com.sam.audioroutine.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.repo.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val latestRoutine: Routine? = null,
    val playbackProgress: PlaybackProgress = PlaybackProgress(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    playbackProgressBus: PlaybackProgressBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playbackProgressBus.progress.collect { progress ->
                _uiState.update { it.copy(playbackProgress = progress) }
            }
        }
        viewModelScope.launch {
            routineRepository.observeLatestRoutine().collect { latest ->
                _uiState.update { it.copy(latestRoutine = latest, isLoading = false) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val latest = routineRepository.getLatestRoutine()
            _uiState.update { it.copy(latestRoutine = latest, isLoading = false) }
        }
    }
}
