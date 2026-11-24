package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.HayatActionType
import com.example.model.HayatAiResult
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "HayatAiEngine"

class HayatAiEngine {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(
                description = "Warm, helpful, natural response from HAYAT in Hindi, English, or Hinglish."
            ),
            "detectedLanguage" to Schema.enumeration(
                values = listOf("en", "hi"),
                description = "Primary language of replyText ('en' for English, 'hi' for Hindi/Devanagari)."
            ),
            "actionType" to Schema.enumeration(
                values = HayatActionType.entries.map { it.name },
                description = "Android device action to execute if the user asked to open an app, play music, call, set alarm/timer, open settings, read notifications, or save reminder; otherwise NONE."
            ),
            "actionTarget" to Schema.string(
                description = "Target parameter for the action: e.g. app name for LAUNCH_APP, search query for PLAY_MUSIC/OPEN_YOUTUBE, phone digits for MAKE_CALL, 'HH:MM|label' for SET_ALARM, minutes integer for SET_TIMER, setting key ('wifi','bluetooth','sound','display','battery','main') for OPEN_SETTINGS, or reminder title for SAVE_REMINDER. Empty string if NONE."
            ),
            "memoryUpdate" to Schema.string(
                description = "Optional short updated memory note about user's preferences or ongoing topic if newly learned, else empty string."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                responseMimeType = "application/json"
                responseSchema = this@HayatAiEngine.responseSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are HAYAT AI, a warm, intelligent, friendly female personal AI assistant on Android.
                    Personality & capabilities:
                    - Speak naturally and kindly like a thoughtful companion.
                    - Understand Hindi (Devanagari and Roman Hinglish) and English fluently. Match the user's preferred language style.
                    - Explain complex topics simply with clear examples when asked to teach or help with study.
                    - Remember conversation history and user profile context provided in the prompt.
                    - When the user asks to perform a phone action (open YouTube, Instagram, WhatsApp, launch an installed app, play music, make a call, set an alarm/timer, open Wi-Fi/Bluetooth/Sound/Display/Battery settings, read notifications, or save a reminder), set the matching `actionType` and `actionTarget`, and confirm naturally in `replyText`.
                    - For `SET_ALARM`, format `actionTarget` as `HH:MM|Label` in 24-hour time (e.g. `07:00|Morning Alarm`).
                    - For `SET_TIMER`, format `actionTarget` as integer seconds or minutes like `10m` or `30s`.
                    - For `OPEN_SETTINGS`, use one of: `wifi`, `bluetooth`, `sound`, `display`, `battery`, `main`.
                    - Keep `replyText` concise enough for pleasant voice playback (2-4 sentences unless the user asks for a detailed explanation or study breakdown).
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateAssistantReply(
        userPrompt: String,
        profile: UserProfile?,
        recentHistory: List<ChatMessage>,
        attachedImage: Bitmap? = null,
        recentNotificationsText: String? = null
    ): HayatAiResult {
        val contextPrompt = buildString {
            appendLine("=== USER CONTEXT ===")
            appendLine("Name: ${profile?.displayName ?: "Friend"}")
            appendLine("Preferred Language: ${profile?.preferredLanguage ?: "hinglish"}")
            appendLine("Current Mood: ${profile?.mood ?: "Focused"}")
            appendLine("City: ${profile?.cityName ?: "Unknown"}")
            if (!profile?.memorySummary.isNullOrBlank()) {
                appendLine("Remembered Facts: ${profile?.memorySummary}")
            }
            if (!recentNotificationsText.isNullOrBlank()) {
                appendLine("Recent Device Notifications (only share if user asked to read notifications):")
                appendLine(recentNotificationsText)
            }
            appendLine("=== CURRENT REQUEST ===")
            append(userPrompt.trim())
        }

        val chatHistory: List<Content> = recentHistory
            .takeLast(12)
            .filter { it.text.isNotBlank() }
            .map { msg ->
                content(role = if (msg.role == "model") "model" else "user") {
                    text(msg.text)
                }
            }

        return try {
            val response = withTimeout(45_000L) {
                if (attachedImage != null) {
                    model.generateContent(
                        content {
                            image(attachedImage)
                            text(contextPrompt)
                        }
                    )
                } else {
                    val chat = model.startChat(history = chatHistory)
                    chat.sendMessage(contextPrompt)
                }
            }

            val rawJson = response.text
                ?: throw IllegalStateException("HAYAT received an empty response from Gemini.")
            parseStructuredResponse(rawJson)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out", e)
            throw IllegalStateException("Connection timed out after 45s. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini inference failed", e)
            throw e
        }
    }

    suspend fun verifyConnection(): Long {
        val start = System.currentTimeMillis()
        return try {
            withTimeout(20_000L) {
                model.generateContent("Respond with a brief friendly status greeting in JSON.")
            }
            (System.currentTimeMillis() - start).coerceAtLeast(1L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "API connection check failed", e)
            throw e
        }
    }

    private fun parseStructuredResponse(rawJson: String): HayatAiResult {
        val json = JSONObject(rawJson)
        val reply = json.optString("replyText").ifBlank {
            "I'm here with you. Could you say that once more?"
        }
        val lang = json.optString("detectedLanguage", "en").let {
            if (it == "hi") "hi" else "en"
        }
        val actionType = HayatActionType.fromName(json.optString("actionType", "NONE"))
        val actionTarget = json.optString("actionTarget", "").trim()
        val memoryUpdate = json.optString("memoryUpdate", "").trim()

        return HayatAiResult(
            replyText = reply,
            detectedLanguage = lang,
            actionType = actionType,
            actionTarget = actionTarget,
            memoryUpdate = memoryUpdate
        )
    }
}
