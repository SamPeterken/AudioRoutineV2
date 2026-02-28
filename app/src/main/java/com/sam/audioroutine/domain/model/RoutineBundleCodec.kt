package com.sam.audioroutine.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Duration

object RoutineBundleCodec {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun encode(routine: Routine): String {
        val payload = buildJsonObject {
            put(ROUTINE_NAME_KEY, routine.name)
            put(
                BLOCKS_KEY,
                buildJsonArray {
                    routine.blocks.forEachIndexed { index, block ->
                        add(
                            buildJsonObject {
                                put(POSITION_KEY, index)
                                put(TEXT_TO_SPEAK_KEY, block.textToSpeak)
                                block.recordedPrompt?.toJsonObjectOrNull()?.let { prompt ->
                                    put(RECORDED_PROMPT_KEY, prompt)
                                }
                                put(WAIT_DURATION_SECONDS_KEY, block.waitDuration.seconds)
                                put(MUSIC_STYLE_KEY, block.musicStyle)
                                block.musicSelection.toJsonObjectOrNull()?.let { selection ->
                                    put(MUSIC_SELECTION_KEY, selection)
                                }
                                put(ADDITIONAL_TTS_EVENTS_KEY, block.additionalTtsEvents.toJsonArray())
                            }
                        )
                    }
                }
            )
        }
        return payload.toString()
    }

    fun decode(payload: String): Routine? {
        if (payload.isBlank()) return null

        return try {
            val rootObject = json.parseToJsonElement(payload).jsonObject
            val routineName = rootObject[ROUTINE_NAME_KEY]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                .orEmpty()
            if (routineName.isBlank()) return null

            val blocks = rootObject[BLOCKS_KEY]
                ?.jsonArray
                ?.mapNotNull { it.toRoutineBlockOrNull() }
                .orEmpty()
                .mapIndexed { index, block -> block.copy(position = index) }

            Routine(name = routineName, blocks = blocks)
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonElement.toRoutineBlockOrNull(): RoutineBlock? {
        val blockObject = jsonObject
        val textToSpeak = blockObject[TEXT_TO_SPEAK_KEY]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            .orEmpty()
        val recordedPrompt = blockObject[RECORDED_PROMPT_KEY]?.toRecordedPromptOrNull()
        if (textToSpeak.isBlank() && recordedPrompt == null) return null

        val waitDurationSeconds = blockObject[WAIT_DURATION_SECONDS_KEY]
            ?.jsonPrimitive
            ?.longOrNull
            ?: return null
        if (waitDurationSeconds < 0L) return null

        val parsedMusicSelection = blockObject[MUSIC_SELECTION_KEY]?.toMusicSelectionOrNull()
        val musicStyle = blockObject[MUSIC_STYLE_KEY]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            ?.ifBlank { null }
            ?: parsedMusicSelection?.displayName

        val additionalEvents = blockObject[ADDITIONAL_TTS_EVENTS_KEY]
            ?.jsonArray
            ?.mapNotNull { it.toRoutineBlockTtsEventOrNull() }
            .orEmpty()
            .sortedBy { it.offsetSeconds }

        val position = blockObject[POSITION_KEY]?.jsonPrimitive?.intOrNull ?: 0

        return RoutineBlock(
            position = position,
            textToSpeak = textToSpeak,
            recordedPrompt = recordedPrompt,
            waitDuration = Duration.ofSeconds(waitDurationSeconds),
            musicStyle = musicStyle,
            musicSelection = parsedMusicSelection,
            additionalTtsEvents = additionalEvents
        )
    }

    private fun JsonElement.toMusicSelectionOrNull(): MusicSelection? {
        val selectionObject = jsonObject
        val source = selectionObject[MUSIC_SOURCE_KEY]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.let(::musicSourceFromNameOrNull)
            ?: return null
        val type = selectionObject[MUSIC_SELECTION_TYPE_KEY]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.let(::musicSelectionTypeFromNameOrNull)
            ?: return null

        return MusicSelection(
            source = source,
            type = type,
            sourceId = selectionObject[MUSIC_SOURCE_ID_KEY]?.jsonPrimitive?.contentOrNull,
            displayName = selectionObject[MUSIC_DISPLAY_NAME_KEY]?.jsonPrimitive?.contentOrNull
        )
    }

    private fun JsonElement.toRoutineBlockTtsEventOrNull(): RoutineBlockTtsEvent? {
        val eventObject = jsonObject
        val offsetSeconds = eventObject[OFFSET_SECONDS_KEY]?.jsonPrimitive?.longOrNull ?: return null
        val text = eventObject[TEXT_KEY]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val recordedPrompt = eventObject[RECORDED_PROMPT_KEY]?.toRecordedPromptOrNull()
        if (offsetSeconds < 0L || (text.isBlank() && recordedPrompt == null)) return null

        return RoutineBlockTtsEvent(
            offsetSeconds = offsetSeconds,
            text = text,
            recordedPrompt = recordedPrompt
        )
    }

    private fun JsonElement.toRecordedPromptOrNull(): RecordedPrompt? {
        val promptObject = jsonObject
        val filePath = promptObject[RECORDED_PROMPT_FILE_PATH_KEY]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            .orEmpty()
        if (filePath.isBlank()) return null
        val durationMillis = promptObject[RECORDED_PROMPT_DURATION_MILLIS_KEY]
            ?.jsonPrimitive
            ?.longOrNull
            ?.coerceAtLeast(0L)
            ?: 0L
        return RecordedPrompt(filePath = filePath, durationMillis = durationMillis)
    }

    private fun MusicSelection?.toJsonObjectOrNull(): JsonObject? {
        if (this == null) return null
        return buildJsonObject {
            put(MUSIC_SOURCE_KEY, source.name)
            put(MUSIC_SELECTION_TYPE_KEY, type.name)
            put(MUSIC_SOURCE_ID_KEY, sourceId)
            put(MUSIC_DISPLAY_NAME_KEY, displayName)
        }
    }

    private fun RecordedPrompt.toJsonObjectOrNull(): JsonObject {
        return buildJsonObject {
            put(RECORDED_PROMPT_FILE_PATH_KEY, filePath)
            put(RECORDED_PROMPT_DURATION_MILLIS_KEY, durationMillis)
        }
    }

    private fun List<RoutineBlockTtsEvent>.toJsonArray(): JsonArray {
        return buildJsonArray {
            this@toJsonArray.forEach { event ->
                add(
                    buildJsonObject {
                        put(OFFSET_SECONDS_KEY, event.offsetSeconds)
                        put(TEXT_KEY, event.text)
                        event.recordedPrompt?.toJsonObjectOrNull()?.let { prompt ->
                            put(RECORDED_PROMPT_KEY, prompt)
                        }
                    }
                )
            }
        }
    }

    private fun musicSourceFromNameOrNull(name: String): MusicSourceType? {
        return try {
            MusicSourceType.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun musicSelectionTypeFromNameOrNull(name: String): MusicSelectionType? {
        return try {
            MusicSelectionType.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private const val ROUTINE_NAME_KEY = "name"
    private const val BLOCKS_KEY = "blocks"
    private const val POSITION_KEY = "position"
    private const val TEXT_TO_SPEAK_KEY = "textToSpeak"
    private const val WAIT_DURATION_SECONDS_KEY = "waitDurationSeconds"
    private const val RECORDED_PROMPT_KEY = "recordedPrompt"
    private const val RECORDED_PROMPT_FILE_PATH_KEY = "filePath"
    private const val RECORDED_PROMPT_DURATION_MILLIS_KEY = "durationMillis"
    private const val MUSIC_STYLE_KEY = "musicStyle"
    private const val MUSIC_SELECTION_KEY = "musicSelection"
    private const val MUSIC_SOURCE_KEY = "source"
    private const val MUSIC_SELECTION_TYPE_KEY = "type"
    private const val MUSIC_SOURCE_ID_KEY = "sourceId"
    private const val MUSIC_DISPLAY_NAME_KEY = "displayName"
    private const val ADDITIONAL_TTS_EVENTS_KEY = "additionalTtsEvents"
    private const val OFFSET_SECONDS_KEY = "offsetSeconds"
    private const val TEXT_KEY = "text"
}
