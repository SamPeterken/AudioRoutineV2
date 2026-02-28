package com.sam.audioroutine.feature.schedule

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.audioroutine.domain.repo.ForegroundTextMode
import com.sam.audioroutine.feature.background.AppBackgroundViewModel
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    backgroundViewModel: AppBackgroundViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backgroundUiState by backgroundViewModel.uiState.collectAsStateWithLifecycle()
    val panelShape = RoundedCornerShape(16.dp)
    val context = LocalContext.current
    val useLightForeground = backgroundUiState.foregroundTextMode == ForegroundTextMode.WHITE
    val primaryTextColor = if (useLightForeground) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (useLightForeground) {
        Color.White.copy(alpha = 0.86f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cardColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    val translucentForegroundColor = MaterialTheme.colorScheme.onSurface
    val translucentSecondaryForegroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val outlinedButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
        contentColor = translucentForegroundColor
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Alarms",
                style = MaterialTheme.typography.headlineMedium,
                color = primaryTextColor
            )
            OutlinedButton(
                onClick = viewModel::addAlarm,
                shape = panelShape,
                colors = outlinedButtonColors,
                border = BorderStroke(1.dp, translucentForegroundColor)
            ) {
                Text("Add alarm")
            }
        }

        uiState.alarms.forEach { alarm ->
            AlarmCard(
                alarm = alarm,
                routines = uiState.routines,
                panelShape = panelShape,
                onToggleExpanded = { viewModel.toggleExpanded(alarm.alarmId) },
                onSetEnabled = { enabled -> viewModel.setEnabled(alarm.alarmId, enabled) },
                onDeleteAlarm = { viewModel.removeAlarm(alarm.alarmId) },
                onSelectRoutine = { routineId -> viewModel.setSelectedRoutine(alarm.alarmId, routineId) },
                onSetTimeMode = { mode -> viewModel.setTimeInputMode(alarm.alarmId, mode) },
                onPickTime = { mode ->
                    showTimePicker(
                        context = context,
                        initialTime = if (mode == TimeInputMode.START) alarm.startTime else viewModel.computedEndTime(alarm),
                        onPicked = { picked -> viewModel.updateTime(alarm.alarmId, mode, picked) }
                    )
                },
                onToggleDay = { day -> viewModel.toggleDay(alarm.alarmId, day) },
                endTime = viewModel.computedEndTime(alarm),
                selectedRoutine = viewModel.selectedRoutine(alarm),
                primaryTextColor = translucentForegroundColor,
                secondaryTextColor = translucentSecondaryForegroundColor,
                cardColor = cardColor,
                outlinedButtonColors = outlinedButtonColors
            )
        }

        if (uiState.showAlarmPermissionsPrompt) {
            AlertDialog(
                onDismissRequest = viewModel::dismissAlarmPermissionsPrompt,
                title = { Text("Allow Alarm Permissions") },
                text = {
                    Text("Android needs alarm permissions to start this routine at the scheduled time.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openExactAlarmSettings(context)
                            viewModel.dismissAlarmPermissionsPrompt()
                        },
                        shape = panelShape
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = viewModel::dismissAlarmPermissionsPrompt,
                        shape = panelShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Not Now")
                    }
                }
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmUiState,
    routines: List<com.sam.audioroutine.domain.model.Routine>,
    panelShape: RoundedCornerShape,
    onToggleExpanded: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onDeleteAlarm: () -> Unit,
    onSelectRoutine: (Long) -> Unit,
    onSetTimeMode: (TimeInputMode) -> Unit,
    onPickTime: (TimeInputMode) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    endTime: java.time.LocalTime,
    selectedRoutine: com.sam.audioroutine.domain.model.Routine?,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardColor: Color,
    outlinedButtonColors: androidx.compose.material3.ButtonColors
) {
    val startText = formatTime(alarm.startTime)
    val endText = formatTime(endTime)
    val nextTriggerText = alarm.nextTrigger?.format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault()))
    val hasRoutines = routines.isNotEmpty()

    Surface(
        shape = panelShape,
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (alarm.isEnabled) {
                    nextTriggerText?.let { "Scheduled: $it" } ?: "Scheduled"
                } else {
                    "Not scheduled"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$startText - $endText",
                    style = MaterialTheme.typography.displaySmall,
                    color = primaryTextColor
                )
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onSetEnabled
                )
            }

            if (alarm.isExpanded) {
                RoutinePicker(
                    routines = routines,
                    selectedRoutineId = alarm.selectedRoutineId,
                    onSelected = onSelectRoutine,
                    enabled = hasRoutines,
                    outlinedButtonColors = outlinedButtonColors
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = alarm.timeInputMode == TimeInputMode.START,
                        onClick = { onSetTimeMode(TimeInputMode.START) },
                        label = { Text("Set Start") }
                    )
                    FilterChip(
                        selected = alarm.timeInputMode == TimeInputMode.END,
                        onClick = { onSetTimeMode(TimeInputMode.END) },
                        label = { Text("Set End") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPickTime(TimeInputMode.START) },
                        enabled = alarm.timeInputMode == TimeInputMode.START,
                        modifier = Modifier.weight(1f),
                        colors = outlinedButtonColors,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Start $startText")
                    }
                    OutlinedButton(
                        onClick = { onPickTime(TimeInputMode.END) },
                        enabled = alarm.timeInputMode == TimeInputMode.END,
                        modifier = Modifier.weight(1f),
                        colors = outlinedButtonColors,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("End $endText")
                    }
                }

                OutlinedButton(
                    onClick = onDeleteAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedButtonColors,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Delete alarm")
                }

                Text(
                    "Days",
                    style = MaterialTheme.typography.labelLarge,
                    color = secondaryTextColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = alarm.selectedDays.contains(day),
                            onClick = { onToggleDay(day) },
                            label = {
                                Text(day.name.first().toString())
                            },
                            modifier = Modifier
                                .width(40.dp)
                                .wrapContentHeight()
                        )
                    }
                }

            }

            if (alarm.statusText.isNotBlank()) {
                Text(
                    alarm.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
        }
    }
}

private fun showTimePicker(
    context: Context,
    initialTime: java.time.LocalTime,
    onPicked: (java.time.LocalTime) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(java.time.LocalTime.of(hour, minute)) },
        initialTime.hour,
        initialTime.minute,
        true
    ).show()
}

@Composable
private fun RoutinePicker(
    routines: List<com.sam.audioroutine.domain.model.Routine>,
    selectedRoutineId: Long?,
    onSelected: (Long) -> Unit,
    enabled: Boolean,
    outlinedButtonColors: androidx.compose.material3.ButtonColors
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = routines.firstOrNull { it.id == selectedRoutineId }?.name ?: "Select routine"

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            colors = outlinedButtonColors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
        ) {
            Text(selectedName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            routines.forEach { routine ->
                DropdownMenuItem(
                    text = { Text(routine.name) },
                    onClick = {
                        onSelected(routine.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatTime(time: java.time.LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

private fun openExactAlarmSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
