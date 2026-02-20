package com.sam.audioroutine.feature.routine

import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import com.sam.audioroutine.domain.repo.RoutineRepository
import com.sam.audioroutine.feature.player.music.FreeCatalogLibrary
import com.sam.audioroutine.feature.player.music.MusicPlaylistCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasCompleteMorningRoutineStarterBlocks() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = RoutineEditorViewModel(repository)
        advanceUntilIdle()

        val blocks = viewModel.uiState.value.blocks

        assertEquals(5, blocks.size)
        assertEquals("Wake up", blocks[0].textToSpeak)
        assertEquals("Drink a glass of water", blocks[1].textToSpeak)
        assertEquals("Exercise", blocks[2].textToSpeak)
        assertEquals("Take a shower", blocks[3].textToSpeak)
        assertEquals("Meditate", blocks[4].textToSpeak)
        assertEquals(MusicSourceType.FREE_CATALOG, blocks[0].musicSelection?.source)
        assertEquals(MusicSelectionType.TRACK, blocks[0].musicSelection?.type)
        assertEquals("soundhelix-1-calm-ambient", blocks[0].musicSelection?.sourceId)
        assertEquals("44m", viewModel.uiState.value.totalDurationText)
    }

    @Test
    fun addBlock_increasesBlockCount() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        val before = viewModel.uiState.value.blocks.size
        viewModel.addBlock()
        val blocks = viewModel.uiState.value.blocks
        val after = blocks.size

        assertEquals(before + 1, after)
    }

    @Test
    fun moveBlock_swapsOrder() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()
        viewModel.addBlock()
        viewModel.updateBlockText(index = 1, value = "Second")

        viewModel.moveBlock(fromIndex = 1, toIndex = 0)

        assertEquals("Second", viewModel.uiState.value.blocks.first().textToSpeak)
    }

    @Test
    fun updateBlockMinutes_updatesTotalDuration() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMinutes(index = 0, minutesInput = "65")

        assertTrue(viewModel.uiState.value.totalDurationText.contains("1h"))
    }

    @Test
    fun updateBlockSeconds_updatesDurationAndRetainsMinuteComponent() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMinutes(index = 0, minutesInput = "2")
        viewModel.updateBlockSeconds(index = 0, secondsInput = "15")

        assertEquals(135L, viewModel.uiState.value.blocks[0].waitDuration.seconds)
        assertTrue(viewModel.uiState.value.totalDurationText.contains("s"))
    }

    @Test
    fun updateBlockMusicStyle_syncsMusicSelection() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMusicStyle(index = 0, value = "Focus")
        assertEquals("Focus", viewModel.uiState.value.blocks[0].musicSelection?.displayName)

        viewModel.updateBlockMusicStyle(index = 0, value = "")
        assertEquals(null, viewModel.uiState.value.blocks[0].musicSelection)
    }

    @Test
    fun setBlockFreeCatalogTrack_setsTrackSelectionAndLegacyMusicStyle() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()
        val track = FreeCatalogLibrary.findTrackById("jazz-uplift-electronic")
        requireNotNull(track)

        viewModel.setBlockFreeCatalogTrack(index = 0, trackId = track.id)

        val block = viewModel.uiState.value.blocks[0]
        assertEquals(track.title, block.musicStyle)
        assertEquals(MusicSourceType.FREE_CATALOG, block.musicSelection?.source)
        assertEquals(MusicSelectionType.TRACK, block.musicSelection?.type)
        assertEquals(track.id, block.musicSelection?.sourceId)
        assertEquals(track.title, block.musicSelection?.displayName)
    }

    @Test
    fun setBlockNoMusic_clearsMusicStyleAndSelection() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.setBlockNoMusic(index = 0)

        val block = viewModel.uiState.value.blocks[0]
        assertEquals(null, block.musicStyle)
        assertEquals(null, block.musicSelection)
    }

    @Test
    fun setBlockLocalFiles_setsLocalSelectionWithEncodedUris() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()
        val localUris = listOf(
            "content://media/external/audio/media/10",
            "content://media/external/audio/media/20"
        )

        viewModel.setBlockLocalFiles(index = 0, fileUris = localUris)

        val block = viewModel.uiState.value.blocks[0]
        assertEquals(null, block.musicStyle)
        assertEquals(MusicSourceType.LOCAL_FILE, block.musicSelection?.source)
        assertEquals(MusicSelectionType.RANDOM_IN_PLAYLIST, block.musicSelection?.type)
        val songs = MusicPlaylistCodec.decode(block.musicSelection?.sourceId)
        assertTrue(songs.any { it.source == MusicSourceType.FREE_CATALOG })
        assertTrue(localUris.all { uri -> songs.any { it.uri == uri } })
    }

    @Test
    fun addAndRemoveBlockSongs_updatesPlaylistSize() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()
        val extraTrack = FreeCatalogLibrary.findTrackById("soundhelix-2-focus-lofi")
        requireNotNull(extraTrack)

        viewModel.addBlockFreeCatalogSong(index = 0, trackId = extraTrack.id)

        val afterAddSongs = MusicPlaylistCodec.decode(viewModel.uiState.value.blocks[0].musicSelection?.sourceId)
        assertTrue(afterAddSongs.any { it.uri == extraTrack.uri })

        viewModel.removeBlockSong(index = 0, songIndex = afterAddSongs.indexOfFirst { it.uri == extraTrack.uri })

        val afterRemoveSongs = MusicPlaylistCodec.decode(viewModel.uiState.value.blocks[0].musicSelection?.sourceId)
        assertTrue(afterRemoveSongs.none { it.uri == extraTrack.uri })
    }

    @Test
    fun setBlockSongOrder_setsPlaylistType() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.setBlockLocalFiles(index = 0, fileUris = listOf("content://media/external/audio/media/99"))
        viewModel.setBlockSongOrder(index = 0, randomOrder = false)
        assertEquals(MusicSelectionType.PLAYLIST, viewModel.uiState.value.blocks[0].musicSelection?.type)

        viewModel.setBlockSongOrder(index = 0, randomOrder = true)
        assertEquals(MusicSelectionType.RANDOM_IN_PLAYLIST, viewModel.uiState.value.blocks[0].musicSelection?.type)
    }

    @Test
    fun addBlockTtsEvent_addsEventWhenWithinBlockDuration() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMinutes(index = 0, minutesInput = "2")
        viewModel.addBlockTtsEvent(index = 0, offsetSecondsInput = "75", text = "Keep moving")

        val events = viewModel.uiState.value.blocks[0].additionalTtsEvents
        assertEquals(1, events.size)
        assertEquals(75L, events[0].offsetSeconds)
        assertEquals("Keep moving", events[0].text)
    }

    @Test
    fun addBlockTtsEvent_ignoresEventsOutsideBlockDuration() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMinutes(index = 0, minutesInput = "1")
        viewModel.addBlockTtsEvent(index = 0, offsetSecondsInput = "61", text = "Too late")

        val events = viewModel.uiState.value.blocks[0].additionalTtsEvents
        assertTrue(events.isEmpty())
    }

    @Test
    fun addBlockCountdownEvent_buildsExpectedCountdownText() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMinutes(index = 0, minutesInput = "3")
        viewModel.addBlockCountdownEvent(index = 0, remainingSecondsInput = "60")

        val events = viewModel.uiState.value.blocks[0].additionalTtsEvents
        assertEquals(1, events.size)
        assertEquals(120L, events[0].offsetSeconds)
        assertEquals("1 minute left", events[0].text)
    }

    @Test
    fun removeBlockTtsEvent_removesOnlyAdditionalEvent() = runTest {
        val viewModel = RoutineEditorViewModel(FakeRoutineRepository())
        advanceUntilIdle()

        viewModel.updateBlockMinutes(index = 0, minutesInput = "2")
        viewModel.addBlockTtsEvent(index = 0, offsetSecondsInput = "30", text = "Half minute")
        viewModel.removeBlockTtsEvent(index = 0, eventIndex = 0)

        val block = viewModel.uiState.value.blocks[0]
        assertTrue(block.additionalTtsEvents.isEmpty())
        assertEquals("Wake up", block.textToSpeak)
    }

    @Test
    fun addRoutine_createsAndSelectsAdditionalRoutine() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = RoutineEditorViewModel(repository)
        advanceUntilIdle()

        val initialCount = viewModel.uiState.value.routines.size
        viewModel.addRoutine()
        advanceUntilIdle()

        assertEquals(initialCount + 1, viewModel.uiState.value.routines.size)
        assertEquals("Routine 2", viewModel.uiState.value.routineName)
    }

    @Test
    fun updateRoutineName_autosavesWithoutManualSave() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = RoutineEditorViewModel(repository)
        advanceUntilIdle()

        viewModel.updateRoutineName("Deep Focus")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals("Deep Focus", repository.getLatestRoutine()?.name)
    }

    @Test
    fun deleteSelectedRoutine_removesCurrentRoutineAndSelectsAnother() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = RoutineEditorViewModel(repository)
        advanceUntilIdle()

        val firstRoutineId = viewModel.uiState.value.selectedRoutineId
        viewModel.addRoutine()
        advanceUntilIdle()
        val secondRoutineId = requireNotNull(viewModel.uiState.value.selectedRoutineId)

        viewModel.deleteSelectedRoutine()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.routines.size)
        assertEquals(firstRoutineId, viewModel.uiState.value.selectedRoutineId)
        assertTrue(repository.getRoutine(secondRoutineId) == null)
    }

}

private class FakeRoutineRepository : RoutineRepository {
    private var nextId = 1L
    private val routines = linkedMapOf<Long, Routine>()
    private val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())

    override fun observeRoutines(): Flow<List<Routine>> = routinesFlow

    override fun observeLatestRoutine(): Flow<Routine?> = routinesFlow.map { list -> list.maxByOrNull { it.id } }

    override suspend fun createRoutine(name: String): Long {
        val routineId = nextId++
        routines[routineId] = Routine(id = routineId, name = name, blocks = emptyList())
        emitRoutines()
        return routineId
    }

    override suspend fun saveRoutine(routine: Routine) {
        val routineId = if (routine.id > 0L) {
            routine.id
        } else {
            nextId++
        }
        routines[routineId] = routine.copy(id = routineId)
        emitRoutines()
    }

    override suspend fun deleteRoutine(routineId: Long) {
        routines.remove(routineId)
        emitRoutines()
    }

    override suspend fun getLatestRoutine(): Routine? = routines.values.maxByOrNull { it.id }

    override suspend fun getRoutine(routineId: Long): Routine? = routines[routineId]

    private fun emitRoutines() {
        routinesFlow.value = routines.values.sortedByDescending { it.id }
    }
}
