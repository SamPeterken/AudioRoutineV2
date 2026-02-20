package com.sam.audioroutine.data.repo

import com.sam.audioroutine.data.db.RoutineBlockEntity
import com.sam.audioroutine.data.db.RoutineDao
import com.sam.audioroutine.data.db.RoutineEntity
import com.sam.audioroutine.data.db.RoutineWithBlocks
import com.sam.audioroutine.domain.model.MusicSelection
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import com.sam.audioroutine.domain.model.Routine
import com.sam.audioroutine.domain.model.RoutineBlock
import com.sam.audioroutine.domain.repo.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        routineDao.observeRoutines().map { routines ->
            routines.map { it.toDomain(emptyList()) }
        }

    override suspend fun createRoutine(name: String): Long {
        return routineDao.upsertRoutine(RoutineEntity(name = name))
    }

    override suspend fun saveRoutine(routine: Routine) {
        val routineId = routineDao.upsertRoutine(RoutineEntity(id = routine.id, name = routine.name))
        val blocks = routine.blocks.mapIndexed { index, block ->
            RoutineBlockEntity(
                id = block.id,
                routineId = if (routine.id == 0L) routineId else routine.id,
                position = index,
                textToSpeak = block.textToSpeak,
                waitDurationSeconds = block.waitDuration.seconds,
                musicStyle = block.musicStyle,
                musicSource = block.musicSelection?.source?.name,
                musicSelectionType = block.musicSelection?.type?.name,
                musicSourceId = block.musicSelection?.sourceId,
                musicDisplayName = block.musicSelection?.displayName,
                additionalTtsEventsJson = RoutineBlockTtsEventsCodec.encode(block.additionalTtsEvents)
            )
        }
        routineDao.replaceBlocks(if (routine.id == 0L) routineId else routine.id, blocks)
    }

    override suspend fun deleteRoutine(routineId: Long) {
        routineDao.deleteRoutineById(routineId)
    }

    override suspend fun getLatestRoutine(): Routine? {
        return routineDao.getLatestRoutineWithBlocks()?.toDomain()
    }

    override fun observeLatestRoutine(): Flow<Routine?> =
        routineDao.observeLatestRoutineWithBlocks().map { routine ->
            routine?.toDomain()
        }

    override suspend fun getRoutine(routineId: Long): Routine? {
        return routineDao.getRoutineWithBlocks(routineId)?.toDomain()
    }

    private fun RoutineEntity.toDomain(blocks: List<RoutineBlockEntity>): Routine =
        Routine(
            id = id,
            name = name,
            blocks = blocks.sortedBy { it.position }.map {
                RoutineBlock(
                    id = it.id,
                    routineId = it.routineId,
                    position = it.position,
                    textToSpeak = it.textToSpeak,
                    waitDuration = Duration.ofSeconds(it.waitDurationSeconds),
                    musicStyle = it.musicStyle,
                    musicSelection = it.toMusicSelectionOrNull(),
                    additionalTtsEvents = RoutineBlockTtsEventsCodec.decode(it.additionalTtsEventsJson)
                )
            }
        )

    private fun RoutineWithBlocks.toDomain(): Routine = routine.toDomain(blocks)

    private fun RoutineBlockEntity.toMusicSelectionOrNull(): MusicSelection? {
        val source = musicSource.toMusicSourceTypeOrNull() ?: return null
        val type = musicSelectionType.toMusicSelectionTypeOrNull() ?: return null
        return MusicSelection(
            source = source,
            type = type,
            sourceId = musicSourceId,
            displayName = musicDisplayName
        )
    }

    private fun String?.toMusicSourceTypeOrNull(): MusicSourceType? {
        if (this.isNullOrBlank()) return null
        return try {
            MusicSourceType.valueOf(this)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun String?.toMusicSelectionTypeOrNull(): MusicSelectionType? {
        if (this.isNullOrBlank()) return null
        return try {
            MusicSelectionType.valueOf(this)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
