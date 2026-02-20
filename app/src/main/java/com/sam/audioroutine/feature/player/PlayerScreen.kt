package com.sam.audioroutine.feature.player

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlayerScreen(
    onOpenActivePlayback: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val panelShape = RoundedCornerShape(4.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Play\nthe ritual", style = MaterialTheme.typography.displaySmall)
        Text(
            "Start your saved sequence and keep the session focused.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val routine = uiState.latestRoutine
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, panelShape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (routine == null) {
                Text(
                    "No saved routine yet. Build one in the Routine tab.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "READY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(routine.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${routine.blocks.size} blocks prepared",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val progress = uiState.playbackProgress
        if (progress.isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.tertiary, panelShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "NOW PLAYING",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(progress.currentPrompt, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Step ${progress.currentBlockIndex + 1} of ${progress.totalBlocks}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = {
                startPlaybackService(context, routine?.id)
                viewModel.refresh()
                onOpenActivePlayback()
            },
            enabled = routine != null,
            modifier = Modifier.fillMaxWidth(),
            shape = panelShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Start Routine")
        }

        Button(
            onClick = { stopPlaybackService(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = panelShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Stop")
        }
    }
}

@Composable
fun ActivePlaybackScreen(
    onExitPlayback: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = uiState.playbackProgress
    val panelShape = RoundedCornerShape(4.dp)
    val finishTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    if (!progress.isRunning) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Routine complete", style = MaterialTheme.typography.displaySmall)
            Text(
                "Playback is not active.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onExitPlayback, modifier = Modifier.fillMaxWidth(), shape = panelShape) {
                Text("Back to Play")
            }
        }
        return
    }

    val currentBlockProgress = calculateProgress(
        total = progress.currentBlockDurationMillis,
        remaining = progress.currentBlockRemainingMillis
    )
    val routineProgress = calculateProgress(
        total = progress.routineDurationMillis,
        remaining = progress.routineRemainingMillis
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(progress.routineName, style = MaterialTheme.typography.headlineMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Finishes", style = MaterialTheme.typography.labelLarge)
                Text(
                    formatClockTime(progress.projectedFinishEpochMillis, finishTimeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, panelShape)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Activity ${progress.currentBlockIndex + 1} of ${progress.totalBlocks}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(progress.currentPrompt, style = MaterialTheme.typography.headlineLarge)
                LinearProgressIndicator(
                    progress = { currentBlockProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${formatDuration(progress.currentBlockRemainingMillis)} left",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        formatDuration(progress.currentBlockDurationMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, panelShape)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Whole Routine", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { routineProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${formatDuration(progress.routineRemainingMillis)} left",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Step ${progress.currentBlockIndex + 1}/${progress.totalBlocks}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text("Upcoming", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, panelShape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(progress.upcomingActivities) { activity ->
                val isCurrent = activity.index == progress.currentBlockIndex
                Column {
                    Text(
                        "${activity.index + 1}. ${activity.prompt}",
                        style = if (isCurrent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        formatDuration(activity.plannedDurationMillis),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { skipCurrentBlock(context) },
                    modifier = Modifier.weight(1f),
                    shape = panelShape
                ) {
                    Text("Skip")
                }
                FilledTonalButton(
                    onClick = { addTime(context, 30_000L) },
                    modifier = Modifier.weight(1f),
                    shape = panelShape
                ) {
                    Text("+30s")
                }
                FilledTonalButton(
                    onClick = { addTime(context, 120_000L) },
                    modifier = Modifier.weight(1f),
                    shape = panelShape
                ) {
                    Text("+2m")
                }
            }
            OutlinedButton(
                onClick = {
                    stopPlaybackService(context)
                    onExitPlayback()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = panelShape
            ) {
                Text("Stop Routine")
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

private fun startPlaybackService(context: Context, routineId: Long?) {
    val intent = Intent(context, RoutinePlaybackService::class.java).apply {
        action = PlaybackServiceContract.ACTION_START
        if (routineId != null) {
            putExtra(PlaybackServiceContract.EXTRA_ROUTINE_ID, routineId)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopPlaybackService(context: Context) {
    val intent = Intent(context, RoutinePlaybackService::class.java).apply {
        action = PlaybackServiceContract.ACTION_STOP
    }
    context.startService(intent)
}

private fun skipCurrentBlock(context: Context) {
    val intent = Intent(context, RoutinePlaybackService::class.java).apply {
        action = PlaybackServiceContract.ACTION_SKIP_CURRENT
    }
    context.startService(intent)
}

private fun addTime(context: Context, addMillis: Long) {
    val intent = Intent(context, RoutinePlaybackService::class.java).apply {
        action = PlaybackServiceContract.ACTION_ADD_TIME
        putExtra(PlaybackServiceContract.EXTRA_ADD_MILLIS, addMillis)
    }
    context.startService(intent)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun calculateProgress(total: Long, remaining: Long): Float {
    if (total <= 0L) return 1f
    val completed = (total - remaining).coerceAtLeast(0L)
    return (completed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

private fun formatClockTime(epochMillis: Long, formatter: DateTimeFormatter): String {
    if (epochMillis <= 0L) return "--:--"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(formatter)
}
