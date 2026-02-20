package com.sam.audioroutine.feature.player.music

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object LocalFileSelectionCodec {
    private const val PREFIX = "localv1:"
    private const val DELIMITER = "|"

    fun encode(uris: List<String>): String? {
        val normalizedUris = uris
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalizedUris.isEmpty()) return null
        val encoded = normalizedUris.joinToString(DELIMITER) { uri ->
            URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
        }
        return PREFIX + encoded
    }

    fun decode(sourceId: String?): List<String> {
        if (sourceId.isNullOrBlank()) return emptyList()
        if (!sourceId.startsWith(PREFIX)) {
            return listOf(sourceId.trim()).filter { it.isNotBlank() }
        }

        return sourceId
            .removePrefix(PREFIX)
            .split(DELIMITER)
            .map { encodedUri ->
                URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString()).trim()
            }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
