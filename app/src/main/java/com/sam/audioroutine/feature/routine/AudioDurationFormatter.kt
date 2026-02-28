package com.sam.audioroutine.feature.routine

fun formatTrackDuration(totalSeconds: Long?): String {
    if (totalSeconds == null || totalSeconds <= 0L) return "?:??"

    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}