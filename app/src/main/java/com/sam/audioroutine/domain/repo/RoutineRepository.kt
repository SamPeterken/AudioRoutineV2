package com.sam.audioroutine.domain.repo

import com.sam.audioroutine.domain.model.Routine
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    fun observeLatestRoutine(): Flow<Routine?>
    suspend fun createRoutine(name: String): Long
    suspend fun saveRoutine(routine: Routine)
    suspend fun deleteRoutine(routineId: Long)
    suspend fun getLatestRoutine(): Routine?
    suspend fun getRoutine(routineId: Long): Routine?
}
