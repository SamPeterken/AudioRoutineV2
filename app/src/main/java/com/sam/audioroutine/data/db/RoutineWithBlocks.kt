package com.sam.audioroutine.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithBlocks(
    @Embedded val routine: RoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val blocks: List<RoutineBlockEntity>
)
