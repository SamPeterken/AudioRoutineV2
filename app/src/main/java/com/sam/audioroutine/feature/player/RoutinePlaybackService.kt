package com.sam.audioroutine.feature.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sam.audioroutine.domain.model.RoutineBlock
import com.sam.audioroutine.domain.model.allTtsEvents
import com.sam.audioroutine.MainActivity
import com.sam.audioroutine.R
import com.sam.audioroutine.domain.repo.RoutineRepository
import com.sam.audioroutine.feature.player.music.BlockMusicResolver
import com.sam.audioroutine.feature.player.music.MusicPromptPolicy
import com.sam.audioroutine.feature.player.music.ResolvedMusic
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class RoutinePlaybackService : Service() {

    @Inject
    lateinit var routineRepository: RoutineRepository

    @Inject
    lateinit var playbackProgressBus: PlaybackProgressBus

    @Inject
    lateinit var blockMusicResolver: BlockMusicResolver

    @Inject
    lateinit var musicPromptPolicy: MusicPromptPolicy

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playbackJob: Job? = null
    private var textToSpeech: TextToSpeech? = null
    private var exoPlayer: ExoPlayer? = null
    private var promptPlayer: ExoPlayer? = null
    private var isTextToSpeechReady = false
    private var currentBlockIndex: Int? = null
    private var skipCurrentBlockRequested = false
    private val additionalDurationMillisByBlockIndex = mutableMapOf<Int, Long>()
    private var elapsedAdjustmentMillis: Long = 0L

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            updateNotification("Music playback error (${error.errorCodeName})")
        }
    }

    private val ttsListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            isTextToSpeechReady = textToSpeech?.let(::configurePreferredTtsLanguage) == true
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing routine"))
        textToSpeech = TextToSpeech(this, ttsListener)
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            volume = 1f
            addListener(playerListener)
        }
        promptPlayer = ExoPlayer.Builder(this).build().apply {
            volume = 1f
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PlaybackServiceContract.ACTION_STOP -> {
                stopSelf()
            }

            PlaybackServiceContract.ACTION_SKIP_CURRENT -> {
                skipCurrentBlockRequested = true
                textToSpeech?.stop()
                promptPlayer?.stop()
            }

            PlaybackServiceContract.ACTION_ADD_TIME -> {
                val addMillis = intent.getLongExtra(PlaybackServiceContract.EXTRA_ADD_MILLIS, 0L)
                    .coerceAtLeast(0L)
                if (addMillis > 0L) {
                    val index = currentBlockIndex
                    if (index != null) {
                        val existing = additionalDurationMillisByBlockIndex[index] ?: 0L
                        additionalDurationMillisByBlockIndex[index] = existing + addMillis
                    }
                }
            }

            PlaybackServiceContract.ACTION_START, null -> {
                val routineId = intent?.getLongExtra(PlaybackServiceContract.EXTRA_ROUTINE_ID, 0L)?.takeIf { it > 0L }
                startPlayback(routineId)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        playbackJob?.cancel()
        serviceScope.cancel()
        currentBlockIndex = null
        skipCurrentBlockRequested = false
        additionalDurationMillisByBlockIndex.clear()
        elapsedAdjustmentMillis = 0L
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech?.setOnUtteranceProgressListener(null)
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.stop()
        exoPlayer?.release()
        promptPlayer?.stop()
        promptPlayer?.release()
        playbackProgressBus.update(PlaybackProgress())
        super.onDestroy()
    }

    private fun startPlayback(routineId: Long?) {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            currentBlockIndex = null
            skipCurrentBlockRequested = false
            additionalDurationMillisByBlockIndex.clear()
            elapsedAdjustmentMillis = 0L

            val routine = if (routineId != null) {
                routineRepository.getRoutine(routineId)
            } else {
                routineRepository.getLatestRoutine()
            }

            if (routine == null || routine.blocks.isEmpty()) {
                updateNotification("No routine found. Create one first.")
                playbackProgressBus.update(PlaybackProgress())
                stopSelf()
                return@launch
            }

            val orderedBlocks = routine.blocks.sortedBy { it.position }
            val routineStartEpochMillis = System.currentTimeMillis()

            orderedBlocks.forEachIndexed { index, block ->
                val resolvedMusic = blockMusicResolver.resolve(block)
                if (resolvedMusic?.isPlayable == true) {
                    playMusic(resolvedMusic)
                }
                runBlockTimeline(
                    routineName = routine.name,
                    orderedBlocks = orderedBlocks,
                    block = block,
                    blockIndex = index,
                    totalBlocks = routine.blocks.size,
                    routineStartEpochMillis = routineStartEpochMillis,
                    musicDisplayName = resolvedMusic?.displayName
                )
            }

            updateNotification("Routine complete")
            delay(1500)
            playbackProgressBus.update(PlaybackProgress())
            stopSelf()
        }
    }

    private suspend fun runBlockTimeline(
        routineName: String,
        orderedBlocks: List<RoutineBlock>,
        block: RoutineBlock,
        blockIndex: Int,
        totalBlocks: Int,
        routineStartEpochMillis: Long,
        musicDisplayName: String?
    ) {
        currentBlockIndex = blockIndex
        skipCurrentBlockRequested = false
        val musicSuffix = musicDisplayName?.takeIf { it.isNotBlank() }
            ?.let { " • Music: $it" }
            .orEmpty()
        val blockStartMillis = System.currentTimeMillis()
        val blockBaseDurationMillis = block.waitDuration.toMillis().coerceAtLeast(0L)
        var activeLine = block.textToSpeak.ifBlank { "Recorded prompt" }

        emitPlaybackSnapshot(
            routineName = routineName,
            orderedBlocks = orderedBlocks,
            blockIndex = blockIndex,
            activeLine = activeLine,
            routineStartEpochMillis = routineStartEpochMillis
        )

        block.allTtsEvents().forEach { event ->
            val targetMillis = event.offsetSeconds * 1000L
            val skippedToNextBlock = waitUntilFixedTarget(
                targetEpochMillis = blockStartMillis + targetMillis,
                blockStartEpochMillis = blockStartMillis,
                blockBaseDurationMillis = blockBaseDurationMillis,
                routineName = routineName,
                orderedBlocks = orderedBlocks,
                blockIndex = blockIndex,
                activeLine = activeLine,
                routineStartEpochMillis = routineStartEpochMillis
            )
            if (skippedToNextBlock) {
                currentBlockIndex = null
                return
            }

            activeLine = event.text.ifBlank { "Recorded prompt" }
            emitPlaybackSnapshot(
                routineName = routineName,
                orderedBlocks = orderedBlocks,
                blockIndex = blockIndex,
                activeLine = activeLine,
                routineStartEpochMillis = routineStartEpochMillis
            )
            updateNotification(
                "$routineName: ${blockIndex + 1}/$totalBlocks ${event.text.ifBlank { "Recorded prompt" }}$musicSuffix"
            )
            duckAndPlayPrompt(event)
            if (skipCurrentBlockRequested) {
                applySkipAdjustment(
                    blockStartEpochMillis = blockStartMillis,
                    blockBaseDurationMillis = blockBaseDurationMillis,
                    blockIndex = blockIndex
                )
                skipCurrentBlockRequested = false
                currentBlockIndex = null
                return
            }
        }

        val skippedToNextBlock = waitUntilBlockEnd(
            blockStartEpochMillis = blockStartMillis,
            blockBaseDurationMillis = blockBaseDurationMillis,
            routineName = routineName,
            orderedBlocks = orderedBlocks,
            blockIndex = blockIndex,
            activeLine = activeLine,
            routineStartEpochMillis = routineStartEpochMillis
        )
        if (skippedToNextBlock) {
            currentBlockIndex = null
            return
        }

        emitPlaybackSnapshot(
            routineName = routineName,
            orderedBlocks = orderedBlocks,
            blockIndex = blockIndex,
            activeLine = activeLine,
            routineStartEpochMillis = routineStartEpochMillis
        )
        currentBlockIndex = null
    }

    private suspend fun waitUntilFixedTarget(
        targetEpochMillis: Long,
        blockStartEpochMillis: Long,
        blockBaseDurationMillis: Long,
        routineName: String,
        orderedBlocks: List<RoutineBlock>,
        blockIndex: Int,
        activeLine: String,
        routineStartEpochMillis: Long
    ): Boolean {
        while (true) {
            if (skipCurrentBlockRequested) {
                applySkipAdjustment(
                    blockStartEpochMillis = blockStartEpochMillis,
                    blockBaseDurationMillis = blockBaseDurationMillis,
                    blockIndex = blockIndex
                )
                skipCurrentBlockRequested = false
                return true
            }

            val remaining = (targetEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            emitPlaybackSnapshot(
                routineName = routineName,
                orderedBlocks = orderedBlocks,
                blockIndex = blockIndex,
                activeLine = activeLine,
                routineStartEpochMillis = routineStartEpochMillis
            )
            if (remaining <= 0L) {
                return false
            }
            delay(remaining.coerceAtMost(1000L))
        }
    }

    private suspend fun waitUntilBlockEnd(
        blockStartEpochMillis: Long,
        blockBaseDurationMillis: Long,
        routineName: String,
        orderedBlocks: List<RoutineBlock>,
        blockIndex: Int,
        activeLine: String,
        routineStartEpochMillis: Long
    ): Boolean {
        while (true) {
            if (skipCurrentBlockRequested) {
                applySkipAdjustment(
                    blockStartEpochMillis = blockStartEpochMillis,
                    blockBaseDurationMillis = blockBaseDurationMillis,
                    blockIndex = blockIndex
                )
                skipCurrentBlockRequested = false
                return true
            }

            val additional = additionalDurationMillisByBlockIndex[blockIndex].orEmptyNonNegative()
            val targetEpochMillis = blockStartEpochMillis + blockBaseDurationMillis + additional
            val remaining = (targetEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)

            emitPlaybackSnapshot(
                routineName = routineName,
                orderedBlocks = orderedBlocks,
                blockIndex = blockIndex,
                activeLine = activeLine,
                routineStartEpochMillis = routineStartEpochMillis
            )

            if (remaining <= 0L) {
                return false
            }
            delay(remaining.coerceAtMost(1000L))
        }
    }

    private fun emitPlaybackSnapshot(
        routineName: String,
        orderedBlocks: List<RoutineBlock>,
        blockIndex: Int,
        activeLine: String,
        routineStartEpochMillis: Long
    ) {
        playbackProgressBus.update(
            PlaybackProgressCalculator.createSnapshot(
                routineName = routineName,
                orderedBlocks = orderedBlocks,
                currentBlockIndex = blockIndex,
                currentLine = activeLine,
                nowEpochMillis = System.currentTimeMillis(),
                routineStartEpochMillis = routineStartEpochMillis,
                additionalDurationMillisByIndex = additionalDurationMillisByBlockIndex,
                elapsedAdjustmentMillis = elapsedAdjustmentMillis
            )
        )
    }

    private fun applySkipAdjustment(
        blockStartEpochMillis: Long,
        blockBaseDurationMillis: Long,
        blockIndex: Int
    ) {
        val additional = additionalDurationMillisByBlockIndex[blockIndex].orEmptyNonNegative()
        val blockEndEpochMillis = blockStartEpochMillis + blockBaseDurationMillis + additional
        val remaining = (blockEndEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        elapsedAdjustmentMillis += remaining
    }

    private fun Long?.orEmptyNonNegative(): Long = this?.coerceAtLeast(0L) ?: 0L

    private fun playMusic(resolvedMusic: ResolvedMusic) {
        val player = exoPlayer ?: return
        val queueUris = resolvedMusic.mediaQueueUris
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (queueUris.isNotEmpty()) {
            val items = queueUris.map(MediaItem::fromUri)
            player.setMediaItems(items, 0, 0L)
            player.shuffleModeEnabled = false
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.prepare()
            player.playWhenReady = true
            return
        }

        val uri = resolvedMusic.mediaUri?.takeIf { it.isNotBlank() } ?: return
        player.setMediaItem(MediaItem.fromUri(uri))
        player.shuffleModeEnabled = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.prepare()
        player.playWhenReady = true
    }

    private fun configurePreferredTtsLanguage(tts: TextToSpeech): Boolean {
        val preferredLocale = Locale.UK
        val preferredResult = tts.setLanguage(preferredLocale)
        if (preferredResult != TextToSpeech.LANG_MISSING_DATA && preferredResult != TextToSpeech.LANG_NOT_SUPPORTED) {
            return true
        }

        val fallbackLocale = Locale.US
        val fallbackResult = tts.setLanguage(fallbackLocale)
        return fallbackResult != TextToSpeech.LANG_MISSING_DATA && fallbackResult != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private suspend fun duckAndPlayPrompt(event: com.sam.audioroutine.domain.model.RoutineBlockTtsEvent) {
        val player = exoPlayer
        if (player != null) {
            player.volume = musicPromptPolicy.onPromptStart(player.volume)
        }
        try {
            val recordedPath = event.recordedPrompt?.filePath?.trim().orEmpty()
            val hasRecorded = recordedPath.isNotBlank() && File(recordedPath).exists()
            if (hasRecorded) {
                playRecordedPromptAndWait(recordedPath)
            } else {
                speakAndWait(event.text)
            }
        } finally {
            if (player != null) {
                player.volume = musicPromptPolicy.onPromptEnd(player.volume)
            }
        }
    }

    private suspend fun playRecordedPromptAndWait(filePath: String) {
        val player = promptPlayer ?: return
        suspendCancellableCoroutine { continuation ->
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                        player.removeListener(this)
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    player.removeListener(this)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }

            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(File(filePath).toUri()))
            player.prepare()
            player.playWhenReady = true

            continuation.invokeOnCancellation {
                player.removeListener(listener)
                player.stop()
            }
        }
        player.clearMediaItems()
    }

    private suspend fun speakAndWait(text: String) {
        val tts = textToSpeech
        if (!isTextToSpeechReady || tts == null || text.isBlank()) {
            return
        }
        suspendCancellableCoroutine { continuation ->
            val utteranceId = "routine-utterance-${System.currentTimeMillis()}"
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            })
            val result = tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )
            if (result == TextToSpeech.ERROR && continuation.isActive) {
                continuation.resume(Unit)
            }
            continuation.invokeOnCancellation {
                tts.setOnUtteranceProgressListener(null)
            }
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Routine Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PLAYBACK, true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Routine")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    companion object {
        private const val CHANNEL_ID = "routine_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
