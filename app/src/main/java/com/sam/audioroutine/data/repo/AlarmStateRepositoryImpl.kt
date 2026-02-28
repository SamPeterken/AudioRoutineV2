package com.sam.audioroutine.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sam.audioroutine.domain.model.AlarmTimeInputMode
import com.sam.audioroutine.domain.model.PersistedAlarm
import com.sam.audioroutine.domain.model.PersistedAlarmState
import com.sam.audioroutine.domain.repo.AlarmStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private val Context.alarmStateDataStore by preferencesDataStore(name = "alarm_state")

class AlarmStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmStateRepository {

    override fun observeAlarmState(): Flow<PersistedAlarmState> {
        return context.alarmStateDataStore.data.map { preferences ->
            val encoded = preferences[ALARM_STATE_KEY] ?: return@map PersistedAlarmState()
            decodeState(encoded) ?: PersistedAlarmState()
        }
    }

    override suspend fun saveAlarmState(state: PersistedAlarmState) {
        val encoded = encodeState(state)
        context.alarmStateDataStore.edit { preferences ->
            preferences[ALARM_STATE_KEY] = encoded
        }
    }

    private fun encodeState(state: PersistedAlarmState): String {
        val root = buildJsonObject {
            put("nextAlarmId", state.nextAlarmId)
            putJsonArray("alarms") {
                state.alarms.forEach { alarm ->
                    add(
                        buildJsonObject {
                            put("alarmId", alarm.alarmId)
                            alarm.selectedRoutineId?.let { put("selectedRoutineId", it) }
                            put("startTime", alarm.startTime.toString())
                            putJsonArray("selectedDays") {
                                alarm.selectedDays
                                    .sortedBy { it.value }
                                    .forEach { add(JsonPrimitive(it.value)) }
                            }
                            put("isEnabled", alarm.isEnabled)
                            put("isExpanded", alarm.isExpanded)
                            put("timeInputMode", alarm.timeInputMode.name)
                            alarm.nextTrigger?.let { put("nextTrigger", it.toString()) }
                            put("statusText", alarm.statusText)
                        }
                    )
                }
            }
        }
        return Json.encodeToString(JsonObject.serializer(), root)
    }

    private fun decodeState(raw: String): PersistedAlarmState? {
        return runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            val nextAlarmId = root["nextAlarmId"]?.jsonPrimitive?.longOrNull ?: 2L
            val alarms = root["alarms"]
                ?.jsonArray
                ?.mapNotNull { decodeAlarm(it) }
                .orEmpty()
            PersistedAlarmState(
                alarms = alarms.ifEmpty { listOf(PersistedAlarm(alarmId = 1L)) },
                nextAlarmId = nextAlarmId
            )
        }.getOrNull()
    }

    private fun decodeAlarm(element: JsonElement): PersistedAlarm? {
        val alarm = element.jsonObject
        val alarmId = alarm["alarmId"]?.jsonPrimitive?.longOrNull ?: return null
        val startTime = alarm["startTime"]?.jsonPrimitive?.contentOrNull
            ?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
            ?: LocalTime.of(7, 0)
        val selectedDays = decodeDays(alarm["selectedDays"]?.jsonArray)
        val mode = alarm["timeInputMode"]?.jsonPrimitive?.contentOrNull
            ?.let { value -> runCatching { AlarmTimeInputMode.valueOf(value) }.getOrNull() }
            ?: AlarmTimeInputMode.START
        val nextTrigger = alarm["nextTrigger"]?.jsonPrimitive?.contentOrNull
            ?.let { value -> runCatching { ZonedDateTime.parse(value) }.getOrNull() }

        return PersistedAlarm(
            alarmId = alarmId,
            selectedRoutineId = alarm["selectedRoutineId"]?.jsonPrimitive?.longOrNull,
            startTime = startTime,
            selectedDays = selectedDays,
            isEnabled = alarm["isEnabled"]?.jsonPrimitive?.booleanOrNull ?: false,
            isExpanded = alarm["isExpanded"]?.jsonPrimitive?.booleanOrNull ?: false,
            timeInputMode = mode,
            nextTrigger = nextTrigger,
            statusText = alarm["statusText"]?.jsonPrimitive?.contentOrNull.orEmpty()
        )
    }

    private fun decodeDays(daysArray: JsonArray?): Set<DayOfWeek> {
        if (daysArray == null) return emptySet()
        return daysArray.mapNotNull { dayElement ->
            val dayValue = dayElement.jsonPrimitive.intOrNull ?: return@mapNotNull null
            runCatching { DayOfWeek.of(dayValue) }.getOrNull()
        }.toSet()
    }

    private companion object {
        val ALARM_STATE_KEY = stringPreferencesKey("alarm_state_json")
    }
}
