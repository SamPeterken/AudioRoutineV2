package com.sam.audioroutine.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY id DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_blocks WHERE routineId = :routineId ORDER BY position ASC")
    fun observeBlocks(routineId: Long): Flow<List<RoutineBlockEntity>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithBlocks(routineId: Long): RoutineWithBlocks?

    @Transaction
    @Query("SELECT * FROM routines ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRoutineWithBlocks(): RoutineWithBlocks?

    @Transaction
    @Query("SELECT * FROM routines ORDER BY id DESC LIMIT 1")
    fun observeLatestRoutineWithBlocks(): Flow<RoutineWithBlocks?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlocks(blocks: List<RoutineBlockEntity>)

    @Query("DELETE FROM routine_blocks WHERE routineId = :routineId")
    suspend fun deleteBlocksByRoutineId(routineId: Long)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: Long)

    @Query("SELECT * FROM routine_schedules WHERE routineId = :routineId")
    fun observeSchedules(routineId: Long): Flow<List<RoutineScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(schedule: RoutineScheduleEntity)

    @Transaction
    suspend fun replaceBlocks(routineId: Long, blocks: List<RoutineBlockEntity>) {
        deleteBlocksByRoutineId(routineId)
        upsertBlocks(blocks)
    }
}
