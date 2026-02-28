package com.sam.audioroutine.feature.routine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.audioroutine.domain.repo.ForegroundTextMode
import com.sam.audioroutine.feature.player.PlaybackServiceContract
import com.sam.audioroutine.feature.player.RoutinePlaybackService
import com.sam.audioroutine.feature.background.AppBackgroundViewModel
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RoutineEditorScreen(
    onOpenActivePlayback: () -> Unit = {},
    viewModel: RoutineEditorViewModel = hiltViewModel(),
    backgroundViewModel: AppBackgroundViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backgroundUiState by backgroundViewModel.uiState.collectAsStateWithLifecycle()
    val latestBlocks by rememberUpdatedState(uiState.blocks)
    val blockShape = RoundedCornerShape(4.dp)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var draggingBlockKey by remember { mutableStateOf<Long?>(null) }
    var draggingBlockIndexFallback by remember { mutableStateOf<Int?>(null) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }
    var shouldFocusAddedBlock by remember { mutableStateOf(false) }
    var previousBlockCount by remember { mutableIntStateOf(uiState.blocks.size) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    var pendingLocalFileBlockIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteBlockIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteRoutineConfirmation by remember { mutableStateOf(false) }
    var showBackgroundSettings by remember { mutableStateOf(false) }
    val useLightForeground = backgroundUiState.foregroundTextMode == ForegroundTextMode.WHITE
    val primaryTextColor = if (useLightForeground) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (useLightForeground) {
        Color.White.copy(alpha = 0.86f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val outlineColor = if (useLightForeground) {
        Color.White.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = outlineColor,
        unfocusedBorderColor = outlineColor,
        focusedTextColor = primaryTextColor,
        unfocusedTextColor = primaryTextColor,
        focusedLabelColor = secondaryTextColor,
        unfocusedLabelColor = secondaryTextColor,
        focusedPlaceholderColor = secondaryTextColor,
        unfocusedPlaceholderColor = secondaryTextColor,
        cursorColor = primaryTextColor
    )
    val translucentForegroundColor = MaterialTheme.colorScheme.onSurface
    val translucentSecondaryForegroundColor = MaterialTheme.colorScheme.onSurfaceVariant
    val translucentTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = translucentForegroundColor,
        unfocusedBorderColor = translucentForegroundColor,
        focusedTextColor = translucentForegroundColor,
        unfocusedTextColor = translucentForegroundColor,
        focusedLabelColor = translucentForegroundColor,
        unfocusedLabelColor = translucentForegroundColor,
        focusedPlaceholderColor = translucentSecondaryForegroundColor,
        unfocusedPlaceholderColor = translucentSecondaryForegroundColor,
        cursorColor = translucentForegroundColor
    )

    val localAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val targetIndex = pendingLocalFileBlockIndex
        pendingLocalFileBlockIndex = null
        if (targetIndex == null || uris.isEmpty()) return@rememberLauncherForActivityResult

        val selectedUriStrings = uris
            .map(Uri::toString)
            .filter { it.isNotBlank() }
            .distinct()
        if (selectedUriStrings.isEmpty()) return@rememberLauncherForActivityResult

        selectedUriStrings.forEach { uriString ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        viewModel.setBlockLocalFiles(index = targetIndex, fileUris = selectedUriStrings)
    }

    val localAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val targetIndex = pendingLocalFileBlockIndex
        if (targetIndex == null) return@rememberLauncherForActivityResult
        if (granted) {
            localAudioPickerLauncher.launch(arrayOf("audio/*"))
        } else {
            pendingLocalFileBlockIndex = null
        }
    }

    val backgroundImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        backgroundViewModel.setBackgroundUri(uri.toString())
    }

    val localImagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            backgroundImagePickerLauncher.launch(arrayOf("image/*"))
        }
    }

    LaunchedEffect(uiState.blocks.size) {
        val hasAddedBlock = uiState.blocks.size > previousBlockCount
        if (hasAddedBlock && shouldFocusAddedBlock && uiState.blocks.isNotEmpty()) {
            val newIndex = uiState.blocks.lastIndex
            expandedIndex = newIndex
            pendingFocusIndex = newIndex
            listState.animateScrollToItem(newIndex)
            draggingBlockKey = null
            draggingBlockIndexFallback = null
            draggingOffsetY = 0f
            shouldFocusAddedBlock = false
        }
        previousBlockCount = uiState.blocks.size
    }

    LaunchedEffect(uiState.selectedRoutineId) {
        expandedIndex = null
        pendingFocusIndex = null
        shouldFocusAddedBlock = false
        draggingBlockKey = null
        draggingBlockIndexFallback = null
        draggingOffsetY = 0f
    }

    val draggingKey = draggingBlockKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Compose your\nroutine",
                    style = MaterialTheme.typography.displaySmall,
                    lineHeight = MaterialTheme.typography.displaySmall.lineHeight,
                    color = primaryTextColor
                )
            }
            IconButton(onClick = { showBackgroundSettings = true }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Background settings",
                    tint = primaryTextColor
                )
            }
        }
        Text(
            text = "Build a deliberate audio sequence with lines, waits, and atmosphere.",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = uiState.routines, key = { it.id }) { routine ->
                val selected = routine.id == uiState.selectedRoutineId
                Button(
                    onClick = { viewModel.selectRoutine(routine.id) },
                    shape = blockShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Text(routine.name)
                }
            }
            item {
                Button(
                    onClick = viewModel::addRoutine,
                    shape = blockShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("+ Routine")
                }
            }
        }

        HorizontalDivider(color = outlineColor)

        if (uiState.isLoading) {
            Text(
                text = "Loading routine...",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )
            return@Column
        }

        OutlinedTextField(
            value = uiState.routineName,
            onValueChange = viewModel::updateRoutineName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Routine Name") },
            shape = blockShape,
            singleLine = true,
            colors = textFieldColors
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, outlineColor, blockShape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "TOTAL DURATION  ${uiState.totalDurationText}",
                style = MaterialTheme.typography.labelLarge,
                color = secondaryTextColor
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = uiState.blocks,
                key = { _, block -> block.id }
            ) { index, block ->
                val itemKey = block.id
                val isExpanded = expandedIndex == index
                val isDragging = draggingKey == itemKey
                val promptFocusRequester = remember { FocusRequester() }
                val dragScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.02f else 1f,
                    label = "drag-scale"
                )
                val dragElevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    label = "drag-elevation"
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (isDragging) draggingOffsetY else 0f
                            scaleX = dragScale
                            scaleY = dragScale
                            shadowElevation = dragElevation.toPx()
                        }
                        .pointerInput(itemKey, expandedIndex) {
                            if (expandedIndex != null) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingBlockKey = itemKey
                                    draggingBlockIndexFallback = index
                                    draggingOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingBlockKey = null
                                    draggingBlockIndexFallback = null
                                    draggingOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggingBlockKey = null
                                    draggingBlockIndexFallback = null
                                    draggingOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val activeKey = draggingBlockKey
                                        ?: return@detectDragGesturesAfterLongPress
                                    val resolvedIndex = latestBlocks.indexOfFirst { it.id == activeKey }
                                    val activeIndex = if (resolvedIndex >= 0) {
                                        resolvedIndex
                                    } else {
                                        draggingBlockIndexFallback ?: -1
                                    }
                                    if (activeIndex == -1) {
                                        draggingBlockKey = null
                                        draggingBlockIndexFallback = null
                                        draggingOffsetY = 0f
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    if (activeIndex !in latestBlocks.indices) {
                                        draggingBlockKey = null
                                        draggingBlockIndexFallback = null
                                        draggingOffsetY = 0f
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    draggingBlockIndexFallback = activeIndex
                                    draggingOffsetY += dragAmount.y

                                    val viewportTop = listState.layoutInfo.viewportStartOffset
                                    val viewportBottom = listState.layoutInfo.viewportEndOffset
                                    val edgeThresholdPx = 96f
                                    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.index == activeIndex
                                    }
                                    val draggedMiddleForAutoScroll = itemInfo?.let {
                                        it.offset + (it.size / 2f) + draggingOffsetY
                                    }
                                    if (draggedMiddleForAutoScroll != null) {
                                        when {
                                            draggedMiddleForAutoScroll < viewportTop + edgeThresholdPx -> {
                                                coroutineScope.launch { listState.scrollBy(-22f) }
                                            }
                                            draggedMiddleForAutoScroll > viewportBottom - edgeThresholdPx -> {
                                                coroutineScope.launch { listState.scrollBy(22f) }
                                            }
                                        }
                                    }

                                    val currentItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.index == activeIndex
                                    } ?: return@detectDragGesturesAfterLongPress

                                    val draggedMiddle = currentItemInfo.offset +
                                        (currentItemInfo.size / 2f) +
                                        draggingOffsetY

                                    val targetItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.index != activeIndex &&
                                            draggedMiddle.toInt() in it.offset..(it.offset + it.size)
                                    } ?: return@detectDragGesturesAfterLongPress

                                    val fromIndex = activeIndex
                                    val toIndex = targetItemInfo.index
                                    viewModel.moveBlock(fromIndex, toIndex)
                                    draggingBlockIndexFallback = toIndex
                                    draggingOffsetY += (currentItemInfo.offset - targetItemInfo.offset).toFloat()
                                }
                            )
                        },
                    shape = blockShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .border(
                                width = if (isDragging) 2.dp else 0.dp,
                                color = if (isDragging) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = blockShape
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DisableSelection {
                            val line = block.title
                                .ifBlank {
                                    if (block.recordedPrompt != null) {
                                        "Recorded prompt"
                                    } else {
                                        block.textToSpeak.ifBlank { "Untitled" }
                                    }
                                }
                            val waitSeconds = block.waitDuration.seconds
                            val waitSummary = if (waitSeconds % 60L == 0L) {
                                "${waitSeconds / 60L}m"
                            } else {
                                "${waitSeconds / 60L}m ${waitSeconds % 60L}s"
                            }
                            val hasMusic = !block.musicStyle.isNullOrBlank() || block.musicSelection != null
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$line • $waitSummary",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = translucentForegroundColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (hasMusic) {
                                    Icon(
                                        imageVector = Icons.Outlined.MusicNote,
                                        contentDescription = "Has music",
                                        tint = translucentForegroundColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        expandedIndex = if (isExpanded) null else index
                                        draggingBlockKey = null
                                        draggingBlockIndexFallback = null
                                        draggingOffsetY = 0f
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse block" else "Expand block",
                                        tint = translucentForegroundColor
                                    )
                                }
                            }
                        }

                        if (isExpanded) {
                            LaunchedEffect(isExpanded, pendingFocusIndex, index) {
                                if (isExpanded && pendingFocusIndex == index) {
                                    promptFocusRequester.requestFocus()
                                    pendingFocusIndex = null
                                }
                            }
                            OutlinedTextField(
                                value = block.title,
                                onValueChange = { viewModel.updateBlockTitle(index, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Block title (not spoken)") },
                                placeholder = {
                                    Text(
                                        "Optional name for this block",
                                        color = translucentSecondaryForegroundColor
                                    )
                                },
                                shape = blockShape,
                                singleLine = true,
                                colors = translucentTextFieldColors
                            )
                            if (block.recordedPrompt == null) {
                                OutlinedTextField(
                                    value = block.textToSpeak,
                                    onValueChange = { viewModel.updateBlockText(index, it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(promptFocusRequester),
                                    label = { Text("Line") },
                                    placeholder = {
                                        Text(
                                            if (index == 0) {
                                                "What would you like to start with?"
                                            } else {
                                                "What would you like to do next?"
                                            },
                                            color = translucentSecondaryForegroundColor
                                        )
                                    },
                                    shape = blockShape,
                                    colors = translucentTextFieldColors,
                                    trailingIcon = {
                                        MicRecordIconButton(
                                            onRecorded = { filePath, durationMillis ->
                                                viewModel.setBlockRecordedPrompt(
                                                    index = index,
                                                    filePath = filePath,
                                                    durationMillis = durationMillis
                                                )
                                            }
                                        )
                                    }
                                )
                            } else {
                                RecordedPromptCompactRow(
                                    filePath = block.recordedPrompt.filePath,
                                    durationMillis = block.recordedPrompt.durationMillis,
                                    onReplaceRecording = { filePath, durationMillis ->
                                        viewModel.setBlockRecordedPrompt(
                                            index = index,
                                            filePath = filePath,
                                            durationMillis = durationMillis
                                        )
                                    },
                                    onDeleteRecording = {
                                        viewModel.clearBlockRecordedPrompt(index)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var waitMinutesInput by remember(index, block.waitDuration.seconds) {
                                    mutableStateOf((block.waitDuration.seconds / 60L).toString())
                                }
                                var waitSecondsInput by remember(index, block.waitDuration.seconds) {
                                    mutableStateOf((block.waitDuration.seconds % 60L).toString())
                                }
                                OutlinedTextField(
                                    value = waitMinutesInput,
                                    onValueChange = {
                                        if (it.isNotEmpty() && !it.all(Char::isDigit)) return@OutlinedTextField
                                        waitMinutesInput = it
                                        if (it.isNotEmpty()) {
                                            viewModel.updateBlockMinutes(index, it)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Wait Min") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = blockShape,
                                    singleLine = true,
                                    colors = translucentTextFieldColors
                                )
                                OutlinedTextField(
                                    value = waitSecondsInput,
                                    onValueChange = {
                                        if (it.isNotEmpty() && !it.all(Char::isDigit)) return@OutlinedTextField
                                        waitSecondsInput = it
                                        if (it.isNotEmpty()) {
                                            viewModel.updateBlockSeconds(index, it)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Wait Sec") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = blockShape,
                                    singleLine = true,
                                    colors = translucentTextFieldColors
                                )
                            }
                            BlockTimedTtsEditor(
                                block = block,
                                blockIndex = index,
                                viewModel = viewModel
                            )
                            BlockMusicEditor(
                                block = block,
                                blockIndex = index,
                                viewModel = viewModel,
                                onPickLocalFiles = { blockIndex ->
                                    pendingLocalFileBlockIndex = blockIndex
                                    val permission = localAudioPermission()
                                    if (permission == null || ContextCompat.checkSelfPermission(
                                            context,
                                            permission
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        localAudioPickerLauncher.launch(arrayOf("audio/*"))
                                    } else {
                                        localAudioPermissionLauncher.launch(permission)
                                    }
                                }
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { pendingDeleteBlockIndex = index },
                                    shape = blockShape,
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier
                                        .width(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete block",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Button(
                                    onClick = { expandedIndex = null },
                                    shape = blockShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
            }

            item(key = "add-block") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 2.dp,
                                color = primaryTextColor,
                                shape = CircleShape
                            )
                            .clickable {
                                shouldFocusAddedBlock = true
                                viewModel.addBlock()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add block",
                            tint = primaryTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (uiState.selectedRoutineId != null) {
                        showDeleteRoutineConfirmation = true
                    }
                },
                shape = blockShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.width(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete routine",
                    modifier = Modifier.size(26.dp)
                )
            }

            Button(
                onClick = {
                    val routineJson = viewModel.selectedRoutineAsJson() ?: return@Button
                    shareRoutineJson(
                        context = context,
                        routineName = uiState.routineName,
                        routineJson = routineJson
                    )
                },
                modifier = Modifier.weight(1f),
                shape = blockShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Share JSON")
            }

            Button(
                onClick = {
                    startPlaybackService(context, uiState.selectedRoutineId)
                    onOpenActivePlayback()
                },
                modifier = Modifier.weight(1f),
                shape = blockShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Play")
            }
        }

        val blockIndexToDelete = pendingDeleteBlockIndex
        if (blockIndexToDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDeleteBlockIndex = null },
                title = { Text("Delete block?") },
                text = { Text("Are you sure you want to delete this block?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeBlock(blockIndexToDelete)
                            expandedIndex = null
                            pendingDeleteBlockIndex = null
                        },
                        shape = blockShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { pendingDeleteBlockIndex = null },
                        shape = blockShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteRoutineConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteRoutineConfirmation = false },
                title = { Text("Delete routine?") },
                text = { Text("Are you sure you want to delete this routine?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSelectedRoutine()
                            showDeleteRoutineConfirmation = false
                        },
                        shape = blockShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteRoutineConfirmation = false },
                        shape = blockShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showBackgroundSettings) {
            AlertDialog(
                onDismissRequest = { showBackgroundSettings = false },
                title = { Text("App background") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pick a photo to use as a blurred background across every screen.")
                        Text("Text color")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = backgroundUiState.foregroundTextMode == ForegroundTextMode.BLACK,
                                onClick = {
                                    backgroundViewModel.setForegroundTextMode(ForegroundTextMode.BLACK)
                                },
                                label = { Text("Black") }
                            )
                            FilterChip(
                                selected = backgroundUiState.foregroundTextMode == ForegroundTextMode.WHITE,
                                onClick = {
                                    backgroundViewModel.setForegroundTextMode(ForegroundTextMode.WHITE)
                                },
                                label = { Text("White") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val permission = localImagePermission()
                            val hasPermission = permission == null || ContextCompat.checkSelfPermission(
                                context,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                backgroundImagePickerLauncher.launch(arrayOf("image/*"))
                            } else {
                                localImagePermissionLauncher.launch(permission)
                            }
                            showBackgroundSettings = false
                        },
                        shape = blockShape
                    ) {
                        Text("Choose photo")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!backgroundUiState.backgroundUri.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    backgroundViewModel.clearBackgroundUri()
                                    showBackgroundSettings = false
                                },
                                shape = blockShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("Remove")
                            }
                        }
                        Button(
                            onClick = { showBackgroundSettings = false },
                            shape = blockShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

private fun shareRoutineJson(context: Context, routineName: String, routineJson: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "$routineName routine")
        putExtra(Intent.EXTRA_TEXT, routineJson)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share routine JSON"))
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

private fun localAudioPermission(): String? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_AUDIO
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun localImagePermission(): String? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_IMAGES
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
