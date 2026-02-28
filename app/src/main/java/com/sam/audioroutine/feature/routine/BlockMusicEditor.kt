package com.sam.audioroutine.feature.routine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sam.audioroutine.data.bundled.BundledMediaCatalog
import com.sam.audioroutine.domain.model.MusicSelectionType
import com.sam.audioroutine.domain.model.MusicSourceType
import com.sam.audioroutine.domain.model.RoutineBlock
import com.sam.audioroutine.feature.player.music.FreeCatalogLibrary
import com.sam.audioroutine.feature.player.music.MusicPlaylistCodec
import com.sam.audioroutine.feature.player.music.PlaylistSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BlockMusicEditor(
    block: RoutineBlock,
    blockIndex: Int,
    viewModel: RoutineEditorViewModel,
    onPickLocalFiles: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSongs = remember(block.musicSelection, block.musicStyle) {
        songsForBlock(block)
    }
    val durationByUri = rememberSongDurationSecondsByUri(context = context, songs = selectedSongs)
    val totalAudioSeconds = remember(selectedSongs, durationByUri.value) {
        if (selectedSongs.isEmpty()) return@remember null
        if (durationByUri.value.size != selectedSongs.size) return@remember null
        selectedSongs.sumOf { song -> durationByUri.value[song.uri] ?: 0L }
    }
    val audioCoverage = remember(totalAudioSeconds, block.waitDuration.seconds) {
        totalAudioSeconds?.let { calculateAudioCoverage(totalAudioSeconds = it, blockSeconds = block.waitDuration.seconds) }
    }
    val isRandomOrder = block.musicSelection?.type != MusicSelectionType.PLAYLIST
    var showSongs by remember(blockIndex, selectedSongs.size) { mutableStateOf(selectedSongs.isNotEmpty()) }
    var addTrackExpanded by remember(blockIndex) { mutableStateOf(false) }
    var addBundledTrackExpanded by remember(blockIndex) { mutableStateOf(false) }
    val allTracks = remember { FreeCatalogLibrary.allTracks() }
    val bundledTracks = remember(context) { BundledMediaCatalog.listBundledAudioTracks(context) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = showSongs,
                onCheckedChange = { checked ->
                    showSongs = checked
                    if (!checked) {
                        viewModel.clearBlockSongs(blockIndex)
                    }
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Song list",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Mix built-in and local songs, then choose playback order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!showSongs) {
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderOptionButton(
                modifier = Modifier.weight(1f),
                selected = !isRandomOrder,
                icon = { Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "In order") },
                label = "In order",
                onClick = { viewModel.setBlockSongOrder(blockIndex, randomOrder = false) }
            )
            OrderOptionButton(
                modifier = Modifier.weight(1f),
                selected = isRandomOrder,
                icon = { Icon(imageVector = Icons.Outlined.Shuffle, contentDescription = "Random") },
                label = "Random",
                onClick = { viewModel.setBlockSongOrder(blockIndex, randomOrder = true) }
            )
        }

        if (selectedSongs.isEmpty()) {
            Text(
                text = "No songs selected yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (audioCoverage != null) {
            Text(
                text = "Audio ${formatTrackDuration(audioCoverage.totalAudioSeconds)} / Block ${formatTrackDuration(audioCoverage.blockSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { audioCoverage.progressFraction },
                modifier = Modifier.fillMaxWidth()
            )
            if (audioCoverage.repeats) {
                Text(
                    text = "Audio is shorter than this block and will repeat.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        selectedSongs.forEachIndexed { songIndex, song ->
            val durationText = formatTrackDuration(durationByUri.value[song.uri])
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = song.title,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    label = { Text(if (song.source == MusicSourceType.FREE_CATALOG) "Built-in" else "Local") },
                    singleLine = true
                )
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { viewModel.removeBlockSong(blockIndex, songIndex) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove song"
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = addTrackExpanded,
            onExpandedChange = { addTrackExpanded = !addTrackExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "Add built-in song",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = addTrackExpanded)
                },
                singleLine = true
            )

            DropdownMenu(
                expanded = addTrackExpanded,
                onDismissRequest = { addTrackExpanded = false }
            ) {
                allTracks.forEach { track ->
                    DropdownMenuItem(
                        text = { Text(track.title) },
                        onClick = {
                            viewModel.addBlockFreeCatalogSong(blockIndex, track.id)
                            addTrackExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = addBundledTrackExpanded,
            onExpandedChange = { addBundledTrackExpanded = !addBundledTrackExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "Add bundled song",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = addBundledTrackExpanded)
                },
                singleLine = true
            )

            DropdownMenu(
                expanded = addBundledTrackExpanded,
                onDismissRequest = { addBundledTrackExpanded = false }
            ) {
                if (bundledTracks.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No bundled songs found in assets/bundled_audio") },
                        onClick = { addBundledTrackExpanded = false }
                    )
                } else {
                    bundledTracks.forEach { track ->
                        DropdownMenuItem(
                            text = { Text(track.title) },
                            onClick = {
                                viewModel.addBlockBundledSong(
                                    index = blockIndex,
                                    title = track.title,
                                    assetUri = track.mediaUri
                                )
                                addBundledTrackExpanded = false
                            }
                        )
                    }
                }
            }
        }

        androidx.compose.material3.Button(
            onClick = { onPickLocalFiles(blockIndex) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add local songs")
        }
    }
}

@Composable
private fun OrderOptionButton(
    modifier: Modifier,
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        icon()
        Text(label)
    }
}

private fun songsForBlock(block: RoutineBlock): List<PlaylistSong> {
    val selection = block.musicSelection
    if (selection?.source == MusicSourceType.LOCAL_FILE) {
        return MusicPlaylistCodec.decode(selection.sourceId)
    }
    if (selection?.source == MusicSourceType.FREE_CATALOG &&
        selection.type == MusicSelectionType.TRACK
    ) {
        val track = selection.sourceId?.let(FreeCatalogLibrary::findTrackById) ?: return emptyList()
        return listOf(
            PlaylistSong(
                source = MusicSourceType.FREE_CATALOG,
                title = track.title,
                uri = track.uri
            )
        )
    }
    return emptyList()
}

@Composable
private fun rememberSongDurationSecondsByUri(context: Context, songs: List<PlaylistSong>): androidx.compose.runtime.State<Map<String, Long?>> {
    val songUris = remember(songs) { songs.map { it.uri } }
    val byUri = remember(songUris) { songs.associateBy { it.uri } }
    return produceState<Map<String, Long?>>(initialValue = emptyMap(), key1 = songUris) {
        value = withContext(Dispatchers.IO) {
            songUris.associateWith { uri ->
                val song = byUri[uri] ?: return@associateWith null
                resolveSongDurationSeconds(context = context, song = song)
            }
        }
    }
}

private fun resolveSongDurationSeconds(context: Context, song: PlaylistSong): Long? {
    val uri = Uri.parse(song.uri)
    val retriever = MediaMetadataRetriever()
    return runCatching {
        val isLocalUri = uri.scheme.equals("content", ignoreCase = true) ||
            uri.scheme.equals("file", ignoreCase = true)
        if (isLocalUri) {
            retriever.setDataSource(context, uri)
        } else {
            retriever.setDataSource(song.uri, emptyMap())
        }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.div(1000L)
            ?.coerceAtLeast(0L)
            ?.takeIf { it > 0L }
    }.getOrNull().also {
        retriever.release()
    }
}
