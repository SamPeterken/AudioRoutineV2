package com.sam.audioroutine.feature.routine

import kotlin.math.min

data class AudioCoverage(
    val totalAudioSeconds: Long,
    val blockSeconds: Long,
    val progressFraction: Float,
    val repeats: Boolean
)

fun calculateAudioCoverage(totalAudioSeconds: Long, blockSeconds: Long): AudioCoverage? {
    if (totalAudioSeconds <= 0L || blockSeconds <= 0L) return null
    val progress = min(totalAudioSeconds.toDouble() / blockSeconds.toDouble(), 1.0).toFloat()
    return AudioCoverage(
        totalAudioSeconds = totalAudioSeconds,
        blockSeconds = blockSeconds,
        progressFraction = progress,
        repeats = totalAudioSeconds < blockSeconds
    )
}