package com.sam.audioroutine.feature.background

import com.sam.audioroutine.domain.repo.AppBackgroundRepository
import com.sam.audioroutine.domain.repo.AppBackgroundSettings
import com.sam.audioroutine.domain.repo.ForegroundTextMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppBackgroundViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setBackgroundUri_updatesState() = runTest {
        val repository = FakeAppBackgroundRepository()
        val viewModel = AppBackgroundViewModel(repository)

        viewModel.setBackgroundUri("content://images/42")
        advanceUntilIdle()

        assertEquals("content://images/42", viewModel.uiState.value.backgroundUri)
    }

    @Test
    fun clearBackgroundUri_removesSavedUri() = runTest {
        val repository = FakeAppBackgroundRepository(initial = "content://images/42")
        val viewModel = AppBackgroundViewModel(repository)

        viewModel.clearBackgroundUri()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.backgroundUri)
    }

    @Test
    fun setForegroundTextMode_updatesState() = runTest {
        val repository = FakeAppBackgroundRepository()
        val viewModel = AppBackgroundViewModel(repository)

        viewModel.setForegroundTextMode(ForegroundTextMode.WHITE)
        advanceUntilIdle()

        assertEquals(ForegroundTextMode.WHITE, viewModel.uiState.value.foregroundTextMode)
    }
}

private class FakeAppBackgroundRepository(
    initial: String? = null
) : AppBackgroundRepository {
    private val flow = MutableStateFlow(
        AppBackgroundSettings(
            backgroundUri = initial,
            foregroundTextMode = ForegroundTextMode.BLACK
        )
    )

    override fun observeBackgroundSettings(): Flow<AppBackgroundSettings> = flow

    override suspend fun setBackgroundUri(uri: String?) {
        flow.value = AppBackgroundSettings(
            backgroundUri = uri,
            foregroundTextMode = flow.value.foregroundTextMode
        )
    }

    override suspend fun setForegroundTextMode(mode: ForegroundTextMode) {
        flow.value = flow.value.copy(foregroundTextMode = mode)
    }
}
