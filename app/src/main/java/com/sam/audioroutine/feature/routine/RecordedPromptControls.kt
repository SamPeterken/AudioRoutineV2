package com.sam.audioroutine.feature.routine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay

@Composable
fun MicRecordIconButton(
    onRecorded: (filePath: String, durationMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var recordingState by remember { mutableStateOf<RecordingState>(RecordingState.Idle) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(recordingState) {
        val state = recordingState
        if (state is RecordingState.Recording) {
            while (recordingState is RecordingState.Recording) {
                elapsedMillis = (SystemClock.elapsedRealtime() - state.startElapsedRealtime).coerceAtLeast(0L)
                delay(200)
            }
        } else {
            elapsedMillis = 0L
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recordingState = startRecording(context)
        } else {
            recordingState = RecordingState.Error("Microphone permission is required.")
        }
    }

    IconButton(
        onClick = {
            showDialog = true
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                recordingState = startRecording(context)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = "Record prompt"
        )
    }

    if (showDialog) {
        val state = recordingState

        AlertDialog(
            onDismissRequest = {
                cancelRecording(recordingState)
                recordingState = RecordingState.Idle
                showDialog = false
            },
            title = { Text("Record prompt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (state) {
                        is RecordingState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is RecordingState.Recording -> {
                            Text(
                                text = "Recording... ${formatDuration(elapsedMillis)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        RecordingState.Idle -> {
                            Text(
                                text = "Preparing recorder...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        text = "Stop to replace this TTS prompt with your voice recording.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = stopRecordingAndKeep(recordingState)
                        recordingState = RecordingState.Idle
                        showDialog = false
                        if (result != null) {
                            onRecorded(result.filePath, result.durationMillis)
                        }
                    },
                    enabled = state is RecordingState.Recording
                ) {
                    Text("Stop & Use")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        cancelRecording(recordingState)
                        recordingState = RecordingState.Idle
                        showDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecordedPromptCompactRow(
    filePath: String,
    durationMillis: Long,
    onReplaceRecording: (filePath: String, durationMillis: Long) -> Unit,
    onDeleteRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mediaPlayer by remember(filePath) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(filePath) { mutableStateOf(false) }
    var playerDurationMillis by remember(filePath) { mutableLongStateOf(durationMillis.coerceAtLeast(0L)) }
    var currentMillis by remember(filePath) { mutableLongStateOf(0L) }
    val recordingFile = remember(filePath) { File(filePath) }
    val fileExists = recordingFile.exists()

    DisposableEffect(filePath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
            currentMillis = position.coerceAtLeast(0L)
            delay(150)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    if (!fileExists) return@IconButton
                    val existingPlayer = mediaPlayer
                    if (existingPlayer != null) {
                        if (existingPlayer.isPlaying) {
                            existingPlayer.pause()
                            isPlaying = false
                        } else {
                            existingPlayer.start()
                            isPlaying = true
                        }
                        return@IconButton
                    }

                    val created = runCatching {
                        MediaPlayer().apply {
                            setDataSource(filePath)
                            prepare()
                            setOnCompletionListener {
                                isPlaying = false
                                currentMillis = 0L
                            }
                        }
                    }.getOrNull() ?: return@IconButton

                    playerDurationMillis = created.duration.toLong().coerceAtLeast(durationMillis)
                    mediaPlayer = created
                    created.start()
                    isPlaying = true
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause recording" else "Play recording"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (fileExists) "Recorded prompt" else "Recording missing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fileExists) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Slider(
                    value = when {
                        playerDurationMillis <= 0L -> 0f
                        else -> (currentMillis.toFloat() / playerDurationMillis.toFloat()).coerceIn(0f, 1f)
                    },
                    onValueChange = { ratio ->
                        if (playerDurationMillis <= 0L) return@Slider
                        val targetMillis = (playerDurationMillis * ratio).toLong().coerceIn(0L, playerDurationMillis)
                        currentMillis = targetMillis
                        mediaPlayer?.seekTo(targetMillis.toInt())
                    },
                    modifier = Modifier.height(24.dp)
                )
                Text(
                    text = "${formatDuration(currentMillis)} / ${formatDuration(playerDurationMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            MicRecordIconButton(
                onRecorded = onReplaceRecording,
                modifier = Modifier.size(36.dp)
            )

            IconButton(
                onClick = {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isPlaying = false
                    currentMillis = 0L
                    onDeleteRecording()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete recording"
                )
            }
        }
    }
}

private sealed class RecordingState {
    data object Idle : RecordingState()
    data class Recording(
        val recorder: MediaRecorder,
        val outputFile: File,
        val startElapsedRealtime: Long
    ) : RecordingState()

    data class Error(val message: String) : RecordingState()
}

private data class RecordingResult(
    val filePath: String,
    val durationMillis: Long
)

private fun startRecording(context: Context): RecordingState {
    val outputFile = createRecordingFile(context)
    val recorder = MediaRecorder()

    return runCatching {
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        RecordingState.Recording(
            recorder = recorder,
            outputFile = outputFile,
            startElapsedRealtime = SystemClock.elapsedRealtime()
        )
    }.getOrElse {
        recorder.release()
        outputFile.delete()
        RecordingState.Error("Could not start recording.")
    }
}

private fun stopRecordingAndKeep(state: RecordingState): RecordingResult? {
    val recordingState = state as? RecordingState.Recording ?: return null
    return runCatching {
        recordingState.recorder.stop()
        recordingState.recorder.release()
        val durationMillis = (SystemClock.elapsedRealtime() - recordingState.startElapsedRealtime).coerceAtLeast(0L)
        RecordingResult(
            filePath = recordingState.outputFile.absolutePath,
            durationMillis = durationMillis
        )
    }.getOrElse {
        recordingState.recorder.release()
        recordingState.outputFile.delete()
        null
    }
}

private fun cancelRecording(state: RecordingState) {
    val recordingState = state as? RecordingState.Recording ?: return
    runCatching {
        recordingState.recorder.stop()
    }
    recordingState.recorder.release()
    recordingState.outputFile.delete()
}

private fun createRecordingFile(context: Context): File {
    val directory = File(context.filesDir, "recorded_prompts")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val filename = "prompt_${UUID.randomUUID()}.m4a"
    return File(directory, filename)
}

private fun formatDuration(durationMillis: Long): String {
    val safeMillis = durationMillis.coerceAtLeast(0L)
    val totalSeconds = safeMillis / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format("%d:%02d", minutes, seconds)
}
