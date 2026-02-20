package com.sam.audioroutine.data.repo

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
                    if (offsetSeconds >= 0L && text.isNotBlank()) {
                        RoutineBlockTtsEvent(offsetSeconds = offsetSeconds, text = text)
                    } else {
                        null
                    }
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private const val OFFSET_SECONDS_KEY = "offsetSeconds"
    private const val TEXT_KEY = "text"
}
