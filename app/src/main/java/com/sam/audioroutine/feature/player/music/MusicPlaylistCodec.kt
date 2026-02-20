package com.sam.audioroutine.feature.player.music

import com.sam.audioroutine.domain.model.MusicSourceType
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PlaylistSong(
    val source: MusicSourceType,
    val title: String,
    val uri: String
)

object MusicPlaylistCodec {
    private const val PLAYLIST_PREFIX = "mixv1:"
    private const val ENTRY_DELIMITER = ";"
    private const val FIELD_DELIMITER = ","

    fun encode(songs: List<PlaylistSong>): String? {
        val normalizedSongs = songs
            .map { song ->
                song.copy(
                    title = song.title.trim(),
                    uri = song.uri.trim()
                )
            }
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.uri }
        if (normalizedSongs.isEmpty()) return null

        val payload = normalizedSongs.joinToString(ENTRY_DELIMITER) { song ->
            listOf(
                encodeField(song.source.name),
                encodeField(song.title),
                encodeField(song.uri)
            ).joinToString(FIELD_DELIMITER)
        }
        return PLAYLIST_PREFIX + payload
    }

    fun decode(sourceId: String?): List<PlaylistSong> {
        if (sourceId.isNullOrBlank()) return emptyList()

        if (sourceId.startsWith(PLAYLIST_PREFIX)) {
            return sourceId.removePrefix(PLAYLIST_PREFIX)
                .split(ENTRY_DELIMITER)
                .mapNotNull(::decodeSongEntry)
                .filter { it.uri.isNotBlank() }
                .distinctBy { it.uri }
        }

        val legacyLocalUris = LocalFileSelectionCodec.decode(sourceId)
        if (legacyLocalUris.isNotEmpty()) {
            return legacyLocalUris.map { uri ->
                PlaylistSong(
                    source = MusicSourceType.LOCAL_FILE,
                    title = localTitleFromUri(uri),
                    uri = uri
                )
            }
        }

        return emptyList()
    }

    private fun decodeSongEntry(entry: String): PlaylistSong? {
        val fields = entry.split(FIELD_DELIMITER)
        if (fields.size < 3) return null
        val sourceName = decodeField(fields[0])
        val source = runCatching { MusicSourceType.valueOf(sourceName) }.getOrNull() ?: return null
        val title = decodeField(fields[1])
        val uri = decodeField(fields[2])
        return PlaylistSong(
            source = source,
            title = title.ifBlank { localTitleFromUri(uri) },
            uri = uri
        )
    }

    private fun encodeField(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun decodeField(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.toString())

    private fun localTitleFromUri(uri: String): String {
        val lastSegment = uri.substringAfterLast('/').substringBefore('?')
        return if (lastSegment.isBlank()) {
            "Local audio"
        } else {
            decodeField(lastSegment)
        }
    }
}