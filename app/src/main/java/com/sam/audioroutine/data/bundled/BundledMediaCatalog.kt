package com.sam.audioroutine.data.bundled

import android.content.Context
import java.util.Locale

data class BundledAudioTrack(
    val title: String,
    val assetPath: String,
    val mediaUri: String
)

object BundledMediaCatalog {
    const val BUNDLED_AUDIO_DIR = "bundled_audio"
    const val DEFAULT_BACKGROUND_ASSET_FILE_NAME = "default_background.jpg"

    fun listBundledAudioTracks(context: Context): List<BundledAudioTrack> {
        val fileNames = runCatching {
            context.assets.list(BUNDLED_AUDIO_DIR).orEmpty().toList()
        }.getOrElse { emptyList() }

        return fileNames
            .filter { isLikelyAudioFile(it) }
            .sorted()
            .map { fileName ->
                val assetPath = "$BUNDLED_AUDIO_DIR/$fileName"
                BundledAudioTrack(
                    title = displayTitleFromFileName(fileName),
                    assetPath = assetPath,
                    mediaUri = assetUri(assetPath)
                )
            }
    }

    fun defaultBackgroundAssetUri(): String {
        return "file:///android_asset/$DEFAULT_BACKGROUND_ASSET_FILE_NAME"
    }

    fun assetUri(assetPath: String): String {
        val normalizedPath = assetPath.trim().removePrefix("/")
        return "asset:///$normalizedPath"
    }

    private fun isLikelyAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.US)
        return extension in setOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
    }

    private fun displayTitleFromFileName(fileName: String): String {
        return fileName
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { fileName }
    }
}
