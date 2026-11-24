package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class HayatLanguage(val code: String, val label: String, val sttLocale: String) {
    ENGLISH("en", "English", "en-IN"),
    HINDI("hi", "हिंदी", "hi-IN"),
    HINGLISH("hinglish", "Hinglish", "hi-IN");

    companion object {
        fun fromCode(code: String?): HayatLanguage =
            entries.firstOrNull { it.code == code } ?: HINGLISH
    }
}

enum class HayatMood(val label: String) {
    FOCUSED("Focused"),
    CHILL("Chill"),
    STUDY("Study"),
    ENERGIZED("Energized");

    companion object {
        fun fromLabel(label: String?): HayatMood =
            entries.firstOrNull { it.label == label } ?: FOCUSED
    }
}

enum class ReminderCategory(val label: String) {
    ALARM("Alarm"),
    REMINDER("Reminder"),
    STUDY("Study"),
    MUSIC("Music");

    companion object {
        fun fromLabel(label: String?): ReminderCategory =
            entries.firstOrNull { it.label == label } ?: REMINDER
    }
}

enum class HayatActionType {
    NONE,
    OPEN_YOUTUBE,
    OPEN_INSTAGRAM,
    OPEN_WHATSAPP,
    LAUNCH_APP,
    MAKE_CALL,
    SET_ALARM,
    SET_TIMER,
    OPEN_SETTINGS,
    PLAY_MUSIC,
    READ_NOTIFICATIONS,
    SAVE_REMINDER;

    companion object {
        fun fromName(raw: String?): HayatActionType =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Friend",
    val preferredLanguage: String = "hinglish",
    val mood: String = "Focused",
    val voiceEnabled: Boolean = true,
    val ttsSpeechRate: Double = 1.0,
    val backgroundAssistantEnabled: Boolean = false,
    val cityName: String = "New Delhi",
    val memorySummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val language: String = "en",
    val actionTag: String? = null,
    val hasImage: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AssistantReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val timeLabel: String = "",
    val category: String = "Reminder",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class HayatAiResult(
    val replyText: String,
    val detectedLanguage: String,
    val actionType: HayatActionType,
    val actionTarget: String,
    val memoryUpdate: String
)

sealed interface ApiConnectionStatus {
    data object Ready : ApiConnectionStatus
    data object Connecting : ApiConnectionStatus
    data class Connected(val latencyMs: Long) : ApiConnectionStatus
    data class Error(val message: String) : ApiConnectionStatus
}

data class WeatherState(
    val city: String = "New Delhi",
    val temperatureC: Double? = null,
    val conditionLabel: String = "Syncing weather...",
    val windKmh: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class NotificationItem(
    val id: String,
    val appName: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postedAtMillis: Long
)

data class InstalledAppItem(
    val appName: String,
    val packageName: String
)

sealed interface UiDataState<out T> {
    data object Loading : UiDataState<Nothing>
    data class Success<T>(val data: T) : UiDataState<T>
    data class Error(val message: String) : UiDataState<Nothing>
}
