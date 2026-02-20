package com.sam.audioroutine.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.model.RoutineBlock
import com.sam.audioroutine.domain.model.RoutineBlockTtsEvent
import com.sam.audioroutine.domain.repo.RoutineRepository
import com.sam.audioroutine.feature.player.music.FreeCatalogLibrary
import com.sam.audioroutine.feature.player.music.FreeCatalogStyle
import com.sam.audioroutine.feature.player.music.FreeCatalogTheme
import com.sam.audioroutine.feature.player.music.LocalFileSelectionCodec
import com.sam.audioroutine.feature.player.music.MusicPlaylistCodec
import com.sam.audioroutine.feature.player.music.PlaylistSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

data class RoutineEditorUiState(
    val routines: List<RoutineListItemUiState> = emptyList(),
    val selectedRoutineId: Long? = null,
    val routineName: String = "Morning Routine",
    val blocks: List<RoutineBlock> = emptyList(),
    val totalDurationText: String = "0m",
    val isSaving: Boolean = false,
    val isLoading: Boolean = true
)

data class RoutineListItemUiState(
    val id: Long,
    val name: String
)

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val defaultWakeTrack =
        FreeCatalogLibrary.findTrackById("soundhelix-1-calm-ambient")
            ?: FreeCatalogLibrary.recommend(FreeCatalogTheme.CALM, FreeCatalogStyle.AMBIENT, energy = 2, limit = 1).first()
    private val defaultWaterTrack =
        FreeCatalogLibrary.findTrackById("soundhelix-2-focus-lofi")
            ?: FreeCatalogLibrary.recommend(FreeCatalogTheme.FOCUS, FreeCatalogStyle.LOFI, energy = 3, limit = 1).first()
    private val defaultExerciseTrack =
        FreeCatalogLibrary.findTrackById("soundhelix-1-uplift-lofi")
            ?: FreeCatalogLibrary.recommend(FreeCatalogTheme.UPLIFT, FreeCatalogStyle.LOFI, energy = 5, limit = 1).first()
    private val defaultShowerTrack =
        FreeCatalogLibrary.findTrackById("jazz-uplift-electronic")
            ?: FreeCatalogLibrary.recommend(FreeCatalogTheme.UPLIFT, FreeCatalogStyle.ELECTRONIC, energy = 4, limit = 1).first()
    private val defaultMeditateTrack =
        FreeCatalogLibrary.findTrackById("sample-piano-mindful")
            ?: FreeCatalogLibrary.recommend(FreeCatalogTheme.MINDFUL, FreeCatalogStyle.PIANO, energy = 1, limit = 1).first()

    private val defaultMorningBlocks: List<RoutineBlock> = listOf(
        RoutineBlock(
            position = 0,
            textToSpeak = "Wake up",
            waitDuration = Duration.ofMinutes(2),
            musicStyle = defaultWakeTrack.title,
            musicSelection = MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = defaultWakeTrack.id,
                displayName = defaultWakeTrack.title
            )
        ),
        RoutineBlock(
            position = 1,
            textToSpeak = "Drink a glass of water",
            waitDuration = Duration.ofMinutes(2),
            musicStyle = defaultWaterTrack.title,
            musicSelection = MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = defaultWaterTrack.id,
                displayName = defaultWaterTrack.title
            )
        ),
        RoutineBlock(
            position = 2,
            textToSpeak = "Exercise",
            waitDuration = Duration.ofMinutes(20),
            musicStyle = defaultExerciseTrack.title,
            musicSelection = MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = defaultExerciseTrack.id,
                displayName = defaultExerciseTrack.title
            )
        ),
        RoutineBlock(
            position = 3,
            textToSpeak = "Take a shower",
            waitDuration = Duration.ofMinutes(10),
            musicStyle = defaultShowerTrack.title,
            musicSelection = MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = defaultShowerTrack.id,
                displayName = defaultShowerTrack.title
            )
        ),
        RoutineBlock(
            position = 4,
            textToSpeak = "Meditate",
            waitDuration = Duration.ofMinutes(10),
            musicStyle = defaultMeditateTrack.title,
            musicSelection = MusicSelection(
                source = MusicSourceType.FREE_CATALOG,
                type = MusicSelectionType.TRACK,
                sourceId = defaultMeditateTrack.id,
                displayName = defaultMeditateTrack.title
            )
        )
    )

    private val _uiState = MutableStateFlow(
        RoutineEditorUiState(
            blocks = defaultMorningBlocks,
            totalDurationText = defaultMorningBlocks.totalDurationText()
        )
    )
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    private var autosaveJob: Job? = null
    private var loadedRoutineId: Long? = null

    init {
        observeRoutines()
    }

    fun addRoutine() {
        viewModelScope.launch {
            val index = _uiState.value.routines.size + 1
            val routineName = "Routine $index"
            val newRoutineId = routineRepository.createRoutine(name = routineName)
            routineRepository.saveRoutine(
                Routine(
                    id = newRoutineId,
                    name = routineName,
                    blocks = emptyList()
                )
            )
            selectRoutine(newRoutineId)
        }
    }

    fun deleteSelectedRoutine() {
        val selectedRoutineId = _uiState.value.selectedRoutineId ?: return
        viewModelScope.launch {
            autosaveJob?.cancel()
            routineRepository.deleteRoutine(selectedRoutineId)
            loadedRoutineId = null
            if (routineRepository.getLatestRoutine() == null) {
                routineRepository.saveRoutine(
                    Routine(
                        name = "Morning Routine",
                        blocks = defaultMorningBlocks.withReindexedPositions()
                    )
                )
            }
        }
    }

    fun selectRoutine(routineId: Long) {
        if (_uiState.value.selectedRoutineId == routineId && loadedRoutineId == routineId) return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedRoutineId = routineId, isLoading = true) }
            loadRoutine(routineId)
        }
    }

    fun updateRoutineName(value: String) {
        _uiState.update { it.copy(routineName = value) }
        scheduleAutosave()
    }

    fun addBlock() {
        _uiState.update { state ->
            val updated = state.blocks + RoutineBlock(
                position = state.blocks.size,
                textToSpeak = "",
                waitDuration = Duration.ofMinutes(1),
                musicStyle = null
            )
            state.copy(blocks = updated.withReindexedPositions(), totalDurationText = updated.totalDurationText())
        }
        scheduleAutosave()
    }

    fun moveBlock(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            if (fromIndex !in state.blocks.indices || toIndex !in state.blocks.indices) return@update state
            if (fromIndex == toIndex) return@update state
            val mutable = state.blocks.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            state.copy(
                blocks = mutable.withReindexedPositions(),
                totalDurationText = mutable.totalDurationText()
            )
        }
        scheduleAutosave()
    }

    fun removeBlock(index: Int) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val updated = state.blocks.toMutableList().apply { removeAt(index) }
            state.copy(
                blocks = updated.withReindexedPositions(),
                totalDurationText = updated.totalDurationText()
            )
        }
        scheduleAutosave()
    }

    fun updateBlockText(index: Int, value: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val updated = state.blocks.toMutableList()
            updated[index] = updated[index].copy(textToSpeak = value)
            state.copy(blocks = updated)
        }
        scheduleAutosave()
    }

    fun updateBlockMinutes(index: Int, minutesInput: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val minutes = minutesInput.toLongOrNull()?.coerceAtLeast(0L) ?: return@update state
            val updated = state.blocks.toMutableList()
            val currentSeconds = updated[index].waitDuration.seconds % 60L
            val updatedBlock = updated[index].copy(
                waitDuration = Duration.ofSeconds((minutes * 60L) + currentSeconds)
            )
            updated[index] = updatedBlock.copy(
                additionalTtsEvents = sanitizeAdditionalEvents(
                    block = updatedBlock,
                    events = updatedBlock.additionalTtsEvents
                )
            )
            state.copy(blocks = updated, totalDurationText = updated.totalDurationText())
        }
        scheduleAutosave()
    }

    fun updateBlockSeconds(index: Int, secondsInput: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val seconds = secondsInput.toLongOrNull()?.coerceAtLeast(0L)?.coerceAtMost(59L) ?: return@update state
            val updated = state.blocks.toMutableList()
            val currentMinutes = updated[index].waitDuration.seconds / 60L
            val updatedBlock = updated[index].copy(
                waitDuration = Duration.ofSeconds((currentMinutes * 60L) + seconds)
            )
            updated[index] = updatedBlock.copy(
                additionalTtsEvents = sanitizeAdditionalEvents(
                    block = updatedBlock,
                    events = updatedBlock.additionalTtsEvents
                )
            )
            state.copy(blocks = updated, totalDurationText = updated.totalDurationText())
        }
        scheduleAutosave()
    }

    fun addBlockTtsEvent(index: Int, offsetSecondsInput: String, text: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val offsetSeconds = offsetSecondsInput.toLongOrNull() ?: return@update state
            if (offsetSeconds < 0L) return@update state
            val sanitizedText = text.trim()
            if (sanitizedText.isBlank()) return@update state

            val updated = state.blocks.toMutableList()
            val block = updated[index]
            if (offsetSeconds > block.waitDuration.seconds) return@update state

            updated[index] = block.copy(
                additionalTtsEvents = sanitizeAdditionalEvents(
                    block = block,
                    events = block.additionalTtsEvents + RoutineBlockTtsEvent(
                        offsetSeconds = offsetSeconds,
                        text = sanitizedText
                    )
                )
            )
            state.copy(blocks = updated)
        }
        scheduleAutosave()
    }

    fun updateBlockTtsEventOffsetSeconds(index: Int, eventIndex: Int, offsetSecondsInput: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            if (eventIndex !in block.additionalTtsEvents.indices) return@update state
            val offsetSeconds = offsetSecondsInput.toLongOrNull() ?: return@update state
            if (offsetSeconds < 0L || offsetSeconds > block.waitDuration.seconds) return@update state

            val updatedEvents = block.additionalTtsEvents.toMutableList()
            updatedEvents[eventIndex] = updatedEvents[eventIndex].copy(offsetSeconds = offsetSeconds)

            val updatedBlocks = state.blocks.toMutableList()
            updatedBlocks[index] = block.copy(
                additionalTtsEvents = sanitizeAdditionalEvents(
                    block = block,
                    events = updatedEvents
                )
            )
            state.copy(blocks = updatedBlocks)
        }
        scheduleAutosave()
    }

    fun updateBlockTtsEventText(index: Int, eventIndex: Int, value: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            if (eventIndex !in block.additionalTtsEvents.indices) return@update state
            val sanitizedText = value.trim()
            if (sanitizedText.isBlank()) return@update state

            val updatedEvents = block.additionalTtsEvents.toMutableList()
            updatedEvents[eventIndex] = updatedEvents[eventIndex].copy(text = sanitizedText)

            val updatedBlocks = state.blocks.toMutableList()
            updatedBlocks[index] = block.copy(
                additionalTtsEvents = sanitizeAdditionalEvents(
                    block = block,
                    events = updatedEvents
                )
            )
            state.copy(blocks = updatedBlocks)
        }
        scheduleAutosave()
    }

    fun removeBlockTtsEvent(index: Int, eventIndex: Int) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            if (eventIndex !in block.additionalTtsEvents.indices) return@update state

            val updatedEvents = block.additionalTtsEvents.toMutableList()
            updatedEvents.removeAt(eventIndex)

            val updatedBlocks = state.blocks.toMutableList()
            updatedBlocks[index] = block.copy(additionalTtsEvents = updatedEvents)
            state.copy(blocks = updatedBlocks)
        }
        scheduleAutosave()
    }

    fun addBlockCountdownEvent(index: Int, remainingSecondsInput: String) {
        val remainingSeconds = remainingSecondsInput.toLongOrNull() ?: return
        addBlockCountdownEvents(index = index, remainingSecondsValues = listOf(remainingSeconds))
    }

    fun addBlockCountdownEvents(index: Int, remainingSecondsValues: List<Long>) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            val countdownEvents = remainingSecondsValues
                .filter { it in 0L..block.waitDuration.seconds }
                .map { remainingSeconds ->
                    val offsetSeconds = block.waitDuration.seconds - remainingSeconds
                    RoutineBlockTtsEvent(
                        offsetSeconds = offsetSeconds,
                        text = formatCountdownText(remainingSeconds)
                    )
                }
            if (countdownEvents.isEmpty()) return@update state

            val updatedBlocks = state.blocks.toMutableList()
            updatedBlocks[index] = block.copy(
                additionalTtsEvents = sanitizeAdditionalEvents(
                    block = block,
                    events = block.additionalTtsEvents + countdownEvents
                )
            )
            state.copy(blocks = updatedBlocks)
        }
        scheduleAutosave()
    }

    fun updateBlockMusicStyle(index: Int, value: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val updated = state.blocks.toMutableList()
            val musicStyle = value.ifBlank { null }
            updated[index] = updated[index].copy(
                musicStyle = musicStyle,
                musicSelection = musicStyle?.let {
                    MusicSelection(
                        source = MusicSourceType.FREE_CATALOG,
                        type = MusicSelectionType.STYLE,
                        displayName = it
                    )
                }
            )
            state.copy(blocks = updated)
        }
        scheduleAutosave()
    }

    fun setBlockNoMusic(index: Int) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val updated = state.blocks.toMutableList()
            updated[index] = updated[index].copy(
                musicStyle = null,
                musicSelection = null
            )
            state.copy(blocks = updated)
        }
        scheduleAutosave()
    }

    fun setBlockFreeCatalogTrack(index: Int, trackId: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val track = FreeCatalogLibrary.findTrackById(trackId) ?: return@update state
            val updated = state.blocks.toMutableList()
            updated[index] = updated[index].copy(
                musicStyle = track.title,
                musicSelection = MusicSelection(
                    source = MusicSourceType.FREE_CATALOG,
                    type = MusicSelectionType.TRACK,
                    sourceId = track.id,
                    displayName = track.title
                )
            )
            state.copy(blocks = updated)
        }
        scheduleAutosave()
    }

    fun addBlockFreeCatalogSong(index: Int, trackId: String) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val track = FreeCatalogLibrary.findTrackById(trackId) ?: return@update state
            val block = state.blocks[index]
            val songs = block.playlistSongs() + PlaylistSong(
                source = MusicSourceType.FREE_CATALOG,
                title = track.title,
                uri = track.uri
            )
            state.withUpdatedBlockMusic(index = index, songs = songs, randomOrder = block.isRandomSongOrder())
        }
        scheduleAutosave()
    }

    fun setBlockLocalFiles(index: Int, fileUris: List<String>) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            val localSongs = fileUris
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .map { uri ->
                    PlaylistSong(
                        source = MusicSourceType.LOCAL_FILE,
                        title = localSongTitle(uri),
                        uri = uri
                    )
                }
            if (localSongs.isEmpty()) return@update state
            val songs = block.playlistSongs() + localSongs
            return@update state.withUpdatedBlockMusic(
                index = index,
                songs = songs,
                randomOrder = block.isRandomSongOrder()
            )
        }
        scheduleAutosave()
    }

    fun removeBlockSong(index: Int, songIndex: Int) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            val songs = block.playlistSongs()
            if (songIndex !in songs.indices) return@update state
            val updatedSongs = songs.toMutableList().apply { removeAt(songIndex) }
            state.withUpdatedBlockMusic(
                index = index,
                songs = updatedSongs,
                randomOrder = block.isRandomSongOrder()
            )
        }
        scheduleAutosave()
    }

    fun setBlockSongOrder(index: Int, randomOrder: Boolean) {
        _uiState.update { state ->
            if (index !in state.blocks.indices) return@update state
            val block = state.blocks[index]
            val songs = block.playlistSongs()
            if (songs.isEmpty()) return@update state
            state.withUpdatedBlockMusic(index = index, songs = songs, randomOrder = randomOrder)
        }
        scheduleAutosave()
    }

    fun clearBlockSongs(index: Int) {
        setBlockNoMusic(index)
    }

    fun getBlockSongs(index: Int): List<PlaylistSong> {
        val block = _uiState.value.blocks.getOrNull(index) ?: return emptyList()
        return block.playlistSongs()
    }

    fun isBlockRandomSongOrder(index: Int): Boolean {
        val block = _uiState.value.blocks.getOrNull(index) ?: return true
        return block.isRandomSongOrder()
    }

    fun saveRoutine() {
        viewModelScope.launch {
            persistCurrentRoutine()
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            ensureSeedRoutineIfEmpty()
            routineRepository.observeRoutines().collect { routines ->
                val routineItems = routines.map {
                    RoutineListItemUiState(id = it.id, name = it.name)
                }
                val currentSelectedId = _uiState.value.selectedRoutineId
                val selectedRoutineId = when {
                    routineItems.any { it.id == currentSelectedId } -> currentSelectedId
                    else -> routineItems.firstOrNull()?.id
                }

                _uiState.update {
                    it.copy(
                        routines = routineItems,
                        selectedRoutineId = selectedRoutineId,
                        isLoading = selectedRoutineId == null
                    )
                }

                if (selectedRoutineId != null && loadedRoutineId != selectedRoutineId) {
                    loadRoutine(selectedRoutineId)
                }
            }
        }
    }

    private suspend fun ensureSeedRoutineIfEmpty() {
        if (routineRepository.getLatestRoutine() != null) return
        routineRepository.saveRoutine(
            Routine(
                name = "Morning Routine",
                blocks = defaultMorningBlocks.withReindexedPositions()
            )
        )
    }

    private suspend fun loadRoutine(routineId: Long) {
        val routine = routineRepository.getRoutine(routineId) ?: return
        loadedRoutineId = routine.id
        val blocks = routine.blocks.withReindexedPositions()
        _uiState.update {
            it.copy(
                selectedRoutineId = routine.id,
                routineName = routine.name,
                blocks = blocks,
                totalDurationText = blocks.totalDurationText(),
                isLoading = false
            )
        }
    }

    private fun scheduleAutosave() {
        val selectedRoutineId = _uiState.value.selectedRoutineId ?: return
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(350)
            persistCurrentRoutine(routineId = selectedRoutineId)
        }
    }

    private suspend fun persistCurrentRoutine(routineId: Long? = _uiState.value.selectedRoutineId) {
        val targetRoutineId = routineId ?: return
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        routineRepository.saveRoutine(
            Routine(
                id = targetRoutineId,
                name = state.routineName,
                blocks = state.blocks.withReindexedPositions()
            )
        )
        _uiState.update { it.copy(isSaving = false) }
    }

    private fun RoutineBlock.playlistSongs(): List<PlaylistSong> {
        val selection = musicSelection
        return when (selection?.source) {
            MusicSourceType.LOCAL_FILE -> MusicPlaylistCodec.decode(selection.sourceId)
            MusicSourceType.FREE_CATALOG -> {
                if (selection.type == MusicSelectionType.TRACK) {
                    val track = selection.sourceId?.let(FreeCatalogLibrary::findTrackById)
                    if (track != null) {
                        listOf(
                            PlaylistSong(
                                source = MusicSourceType.FREE_CATALOG,
                                title = track.title,
                                uri = track.uri
                            )
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
    }

    private fun RoutineBlock.isRandomSongOrder(): Boolean {
        return musicSelection?.type != MusicSelectionType.PLAYLIST
    }

    private fun RoutineEditorUiState.withUpdatedBlockMusic(
        index: Int,
        songs: List<PlaylistSong>,
        randomOrder: Boolean
    ): RoutineEditorUiState {
        if (songs.isEmpty()) {
            val clearedBlocks = blocks.toMutableList()
            clearedBlocks[index] = clearedBlocks[index].copy(
                musicStyle = null,
                musicSelection = null
            )
            return copy(blocks = clearedBlocks)
        }

        val normalizedSongs = songs
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.uri }
        val encodedPlaylist = MusicPlaylistCodec.encode(normalizedSongs)
            ?: LocalFileSelectionCodec.encode(normalizedSongs.map { it.uri })
            ?: return this
        val displayName = when (normalizedSongs.size) {
            1 -> normalizedSongs.first().title
            else -> "${normalizedSongs.size} songs"
        }

        val updatedBlocks = blocks.toMutableList()
        updatedBlocks[index] = updatedBlocks[index].copy(
            musicStyle = null,
            musicSelection = MusicSelection(
                source = MusicSourceType.LOCAL_FILE,
                type = if (randomOrder) MusicSelectionType.RANDOM_IN_PLAYLIST else MusicSelectionType.PLAYLIST,
                sourceId = encodedPlaylist,
                displayName = displayName
            )
        )
        return copy(blocks = updatedBlocks)
    }

    private fun localSongTitle(uri: String): String {
        val lastSegment = uri.substringAfterLast('/').substringBefore('?')
        return if (lastSegment.isBlank()) "Local audio" else lastSegment
    }

    private fun List<RoutineBlock>.withReindexedPositions(): List<RoutineBlock> {
        return mapIndexed { index, block -> block.copy(position = index) }
    }

    private fun sanitizeAdditionalEvents(
        block: RoutineBlock,
        events: List<RoutineBlockTtsEvent>
    ): List<RoutineBlockTtsEvent> {
        return events
            .filter { it.offsetSeconds in 0L..block.waitDuration.seconds && it.text.isNotBlank() }
            .sortedBy { it.offsetSeconds }
    }

    private fun formatCountdownText(remainingSeconds: Long): String {
        val minutes = remainingSeconds / 60L
        val seconds = remainingSeconds % 60L
        return when {
            minutes > 0L && seconds == 0L -> {
                if (minutes == 1L) "1 minute left" else "$minutes minutes left"
            }

            minutes == 0L && seconds == 1L -> "1 second left"
            minutes == 0L -> "$seconds seconds left"
            else -> "${minutes}m ${seconds}s left"
        }
    }

    private fun List<RoutineBlock>.totalDurationText(): String {
        val totalSeconds = sumOf { it.waitDuration.seconds }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            if (seconds == 0L) {
                "${hours}h ${minutes}m"
            } else {
                "${hours}h ${minutes}m ${seconds}s"
            }
        } else if (minutes > 0) {
            if (seconds == 0L) {
                "${minutes}m"
            } else {
                "${minutes}m ${seconds}s"
            }
        } else {
            "${seconds}s"
        }
    }
}
