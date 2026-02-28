package com.sam.audioroutine.domain.repo

import com.sam.audioroutine.domain.model.PersistedAlarmState
import kotlinx.coroutines.flow.Flow

interface AlarmStateRepository {
    fun observeAlarmState(): Flow<PersistedAlarmState>
    suspend fun saveAlarmState(state: PersistedAlarmState)
}
