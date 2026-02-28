package com.sam.audioroutine.data.repo

import com.sam.audioroutine.domain.model.RecordedPrompt
import com.sam.audioroutine.domain.model.RoutineBlockTtsEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal object RoutineBlockTtsEventsCodec {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun encode(events: List<RoutineBlockTtsEvent>): String? {
        if (events.isEmpty()) return null
        val payload = buildJsonArray {
            events.forEach { event ->
                add(
                    buildJsonObject {
                        put(OFFSET_SECONDS_KEY, event.offsetSeconds)
                        put(TEXT_KEY, event.text)
                        event.recordedPrompt?.let { prompt ->
                            put(RECORDED_PROMPT_FILE_PATH_KEY, prompt.filePath)
                            put(RECORDED_PROMPT_DURATION_MILLIS_KEY, prompt.durationMillis)
                        }
                    }
                )
            }
        }
        return payload.toString()
    }

    fun decode(value: String?): List<RoutineBlockTtsEvent> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.parseToJsonElement(value)
                .jsonArray
                .mapNotNull { element ->
                    val item = element.jsonObject
                    val offsetSeconds = item[OFFSET_SECONDS_KEY]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    val text = item[TEXT_KEY]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    val recordedPrompt = item.toRecordedPromptOrNull()
                    if (offsetSeconds >= 0L && (text.isNotBlank() || recordedPrompt != null)) {
                        RoutineBlockTtsEvent(
                            offsetSeconds = offsetSeconds,
                            text = text,
                            recordedPrompt = recordedPrompt
                        )
                    } else {
                        null
                    }
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun Map<String, kotlinx.serialization.json.JsonElement>.toRecordedPromptOrNull(): RecordedPrompt? {
        val filePath = this[RECORDED_PROMPT_FILE_PATH_KEY]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            .orEmpty()
        if (filePath.isBlank()) return null
        val durationMillis = this[RECORDED_PROMPT_DURATION_MILLIS_KEY]
            ?.jsonPrimitive
            ?.longOrNull
            ?.coerceAtLeast(0L)
            ?: 0L
        return RecordedPrompt(filePath = filePath, durationMillis = durationMillis)
    }

    private const val OFFSET_SECONDS_KEY = "offsetSeconds"
    private const val TEXT_KEY = "text"
    private const val RECORDED_PROMPT_FILE_PATH_KEY = "recordedPromptFilePath"
    private const val RECORDED_PROMPT_DURATION_MILLIS_KEY = "recordedPromptDurationMillis"
}
