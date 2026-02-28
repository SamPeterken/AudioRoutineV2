package com.sam.audioroutine.feature.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.audioroutine.domain.model.RoutineBlock
import java.util.Locale
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun BlockTimedTtsEditor(
    block: RoutineBlock,
    blockIndex: Int,
    viewModel: RoutineEditorViewModel,
    modifier: Modifier = Modifier
) {
    var showTimedTts by remember(blockIndex, block.additionalTtsEvents.size) {
        mutableStateOf(block.additionalTtsEvents.isNotEmpty())
    }
    var newOffsetMinutes by remember(blockIndex) { mutableStateOf("0") }
    var newOffsetSeconds by remember(blockIndex) { mutableStateOf("0") }
    var newEventText by remember(blockIndex) { mutableStateOf("") }
    var addCountdowns by remember(blockIndex) { mutableStateOf(false) }
    var nextCountdownId by remember(blockIndex) { mutableLongStateOf(3L) }
    val countdownDrafts = remember(blockIndex) {
        mutableStateListOf(
            CountdownDraft(id = 1L, minutes = "1", seconds = "0"),
            CountdownDraft(id = 2L, minutes = "0", seconds = "30")
        )
    }

    fun resetCountdownDrafts() {
        countdownDrafts.clear()
        countdownDrafts.add(CountdownDraft(id = 1L, minutes = "1", seconds = "0"))
        countdownDrafts.add(CountdownDraft(id = 2L, minutes = "0", seconds = "30"))
        nextCountdownId = 3L
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = showTimedTts,
                onCheckedChange = { checked ->
                    showTimedTts = checked
                    if (!checked) {
                        addCountdowns = false
                    }
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Add more lines",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Optional. A start line at 0m 0s is always included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!showTimedTts) {
            return
        }

        Text(
            text = "Scheduled lines",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (block.recordedPrompt != null) {
                "0m 0s • Recorded prompt"
            } else {
                "0m 0s • ${block.textToSpeak.ifBlank { "(empty start line)" }}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        block.additionalTtsEvents.forEachIndexed { eventIndex, event ->
            val eventMinutes = event.offsetSeconds / 60L
            val eventSeconds = event.offsetSeconds % 60L
            var eventMinutesInput by remember(blockIndex, eventIndex, event.offsetSeconds) {
                mutableStateOf(eventMinutes.toString())
            }
            var eventSecondsInput by remember(blockIndex, eventIndex, event.offsetSeconds) {
                mutableStateOf(eventSeconds.toString())
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = eventMinutesInput,
                        onValueChange = {
                            if (it.isNotEmpty() && !it.all(Char::isDigit)) return@OutlinedTextField
                            eventMinutesInput = it
                            val minutes = it.toLongOrNull() ?: return@OutlinedTextField
                            val seconds = eventSecondsInput.toLongOrNull() ?: return@OutlinedTextField
                            val totalSeconds = (minutes * 60L) + seconds.coerceAtLeast(0L)
                            viewModel.updateBlockTtsEventOffsetSeconds(
                                index = blockIndex,
                                eventIndex = eventIndex,
                                offsetSecondsInput = totalSeconds.toString()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = eventSecondsInput,
                        onValueChange = {
                            if (it.isNotEmpty() && !it.all(Char::isDigit)) return@OutlinedTextField
                            eventSecondsInput = it
                            val minutes = eventMinutesInput.toLongOrNull() ?: return@OutlinedTextField
                            val seconds = it.toLongOrNull() ?: return@OutlinedTextField
                            val totalSeconds = (minutes * 60L) + seconds.coerceAtLeast(0L)
                            viewModel.updateBlockTtsEventOffsetSeconds(
                                index = blockIndex,
                                eventIndex = eventIndex,
                                offsetSecondsInput = totalSeconds.toString()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { viewModel.removeBlockTtsEvent(index = blockIndex, eventIndex = eventIndex) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove scheduled line"
                        )
                    }
                }
                if (event.recordedPrompt == null) {
                    OutlinedTextField(
                        value = event.text,
                        onValueChange = {
                            viewModel.updateBlockTtsEventText(
                                index = blockIndex,
                                eventIndex = eventIndex,
                                value = it
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Line") },
                        singleLine = true,
                        trailingIcon = {
                            MicRecordIconButton(
                                onRecorded = { filePath, durationMillis ->
                                    viewModel.setBlockTtsEventRecordedPrompt(
                                        index = blockIndex,
                                        eventIndex = eventIndex,
                                        filePath = filePath,
                                        durationMillis = durationMillis
                                    )
                                }
                            )
                        }
                    )
                } else {
                    RecordedPromptCompactRow(
                        filePath = event.recordedPrompt.filePath,
                        durationMillis = event.recordedPrompt.durationMillis,
                        onReplaceRecording = { filePath, durationMillis ->
                            viewModel.setBlockTtsEventRecordedPrompt(
                                index = blockIndex,
                                eventIndex = eventIndex,
                                filePath = filePath,
                                durationMillis = durationMillis
                            )
                        },
                        onDeleteRecording = {
                            viewModel.clearBlockTtsEventRecordedPrompt(
                                index = blockIndex,
                                eventIndex = eventIndex
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newOffsetMinutes,
                onValueChange = { newOffsetMinutes = it },
                modifier = Modifier.weight(1f),
                label = { Text("At min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = newOffsetSeconds,
                onValueChange = { newOffsetSeconds = it },
                modifier = Modifier.weight(1f),
                label = { Text("At sec") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = newEventText,
            onValueChange = { newEventText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Line text") },
            singleLine = true,
            trailingIcon = {
                MicRecordIconButton(
                    onRecorded = { filePath, durationMillis ->
                        val minutes = newOffsetMinutes.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                        val seconds = newOffsetSeconds.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                        val totalSeconds = (minutes * 60L) + seconds
                        viewModel.addBlockRecordedTtsEvent(
                            index = blockIndex,
                            offsetSecondsInput = totalSeconds.toString(),
                            filePath = filePath,
                            durationMillis = durationMillis
                        )
                        newEventText = ""
                    }
                )
            }
        )
        Button(
            onClick = {
                val minutes = newOffsetMinutes.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val seconds = newOffsetSeconds.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val totalSeconds = (minutes * 60L) + seconds
                viewModel.addBlockTtsEvent(
                    index = blockIndex,
                    offsetSecondsInput = totalSeconds.toString(),
                    text = newEventText
                )
                newEventText = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Line")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = addCountdowns,
                onCheckedChange = { checked ->
                    addCountdowns = checked
                    if (checked) {
                        resetCountdownDrafts()
                    }
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Add countdowns",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Edit countdown times, then press Done to add them to scheduled lines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (addCountdowns) {
            countdownDrafts.forEachIndexed { index, draft ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft.minutes,
                        onValueChange = {
                            countdownDrafts[index] = draft.copy(minutes = it)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Remaining min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.seconds,
                        onValueChange = {
                            countdownDrafts[index] = draft.copy(seconds = it)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Remaining sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            countdownDrafts.removeAll { item -> item.id == draft.id }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove countdown"
                        )
                    }
                }
            }

            Button(
                onClick = {
                    countdownDrafts += CountdownDraft(
                        id = nextCountdownId,
                        minutes = "0",
                        seconds = "0"
                    )
                    nextCountdownId += 1L
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add countdown"
                )
                Text("Add Countdown")
            }

            Button(
                onClick = {
                    val remainingSecondsValues = countdownDrafts.mapNotNull { draft ->
                        val minutes = draft.minutes.toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null
                        val seconds = draft.seconds.toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null
                        (minutes * 60L) + seconds
                    }
                    viewModel.addBlockCountdownEvents(
                        index = blockIndex,
                        remainingSecondsValues = remainingSecondsValues
                    )
                    addCountdowns = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Done")
            }
        }

        Text(
            text = "Block length: ${formatMinutesSeconds(block.waitDuration.seconds)}. Events outside this range are ignored.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMinutesSeconds(totalSeconds: Long): String {
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (seconds == 0L) {
        String.format(Locale.US, "%dm", minutes)
    } else {
        String.format(Locale.US, "%dm %ds", minutes, seconds)
    }
}

private data class CountdownDraft(
    val id: Long,
    val minutes: String,
    val seconds: String
)
