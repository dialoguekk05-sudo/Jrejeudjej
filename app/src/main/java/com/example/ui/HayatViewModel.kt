package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HayatAiEngine
import com.example.data.HayatFirestoreRepository
import com.example.data.WeatherRepository
import com.example.model.ApiConnectionStatus
import com.example.model.AssistantReminder
import com.example.model.ChatMessage
import com.example.model.HayatActionType
import com.example.model.HayatLanguage
import com.example.model.HayatMood
import com.example.model.UiDataState
import com.example.model.UserProfile
import com.example.model.WeatherState
import com.example.service.HayatAssistantService
import com.example.service.HayatBackgroundBridge
import com.example.service.NotificationCache
import com.example.voice.HayatVoiceController
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "HayatViewModel"
private const val STOP_TIMEOUT_MS = 5000L

data class DeviceActionEvent(
    val actionType: HayatActionType,
    val target: String
)

class HayatViewModel(
    private val repository: HayatFirestoreRepository,
    private val aiEngine: HayatAiEngine,
    private val weatherRepository: WeatherRepository,
    val voiceController: HayatVoiceController,
    private val currentUserId: String,
    defaultDisplayName: String
) : ViewModel() {

    val profileState: StateFlow<UiDataState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiDataState<UserProfile?>> { UiDataState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiDataState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiDataState.Loading)

    val messagesState: StateFlow<UiDataState<List<ChatMessage>>> = repository.observeMessages(currentUserId)
        .map<List<ChatMessage>, UiDataState<List<ChatMessage>>> { UiDataState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing messages", root)
            emit(UiDataState.Error(root.message ?: "Failed to load chat history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiDataState.Loading)

    val remindersState: StateFlow<UiDataState<List<AssistantReminder>>> = repository.observeReminders(currentUserId)
        .map<List<AssistantReminder>, UiDataState<List<AssistantReminder>>> { UiDataState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing reminders", root)
            emit(UiDataState.Error(root.message ?: "Failed to load reminders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiDataState.Loading)

    private val _weatherState = MutableStateFlow(WeatherState(isLoading = true))
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _apiStatus = MutableStateFlow<ApiConnectionStatus>(ApiConnectionStatus.Ready)
    val apiStatus: StateFlow<ApiConnectionStatus> = _apiStatus.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _actionEvents = MutableSharedFlow<DeviceActionEvent>(extraBufferCapacity = 4)
    val actionEvents: SharedFlow<DeviceActionEvent> = _actionEvents.asSharedFlow()

    private var lastLoadedCity: String? = null

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, defaultDisplayName)
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Profile sync failed."
                }
        }

        viewModelScope.launch {
            profileState.collect { state ->
                val profile = (state as? UiDataState.Success)?.data ?: return@collect
                voiceController.updateSpeechRate(profile.ttsSpeechRate.toFloat())
                val city = profile.cityName.ifBlank { "New Delhi" }
                if (city != lastLoadedCity) {
                    lastLoadedCity = city
                    refreshWeather(city)
                }
            }
        }

        viewModelScope.launch {
            HayatBackgroundBridge.backgroundCommands.collect { command ->
                when (command) {
                    HayatAssistantService.CMD_MUTE -> voiceController.stopSpeaking()
                    HayatAssistantService.CMD_LISTEN -> startVoiceCapture()
                }
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun refreshWeather(cityOverride: String? = null) {
        val currentProfile = (profileState.value as? UiDataState.Success)?.data
        val city = (cityOverride ?: currentProfile?.cityName ?: "New Delhi").ifBlank { "New Delhi" }
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(city = city, isLoading = true, errorMessage = null)
            _weatherState.value = weatherRepository.fetchWeatherForCity(city)
        }
    }

    fun refreshWeatherWithCoordinates(lat: Double, lon: Double, label: String = "Current Location") {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true, errorMessage = null)
            _weatherState.value = weatherRepository.fetchWeatherByCoordinates(lat, lon, label)
        }
    }

    fun testApiConnection() {
        viewModelScope.launch {
            _apiStatus.value = ApiConnectionStatus.Connecting
            try {
                val latency = aiEngine.verifyConnection()
                _apiStatus.value = ApiConnectionStatus.Connected(latency)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Gemini connection failed"
                _apiStatus.value = ApiConnectionStatus.Error(msg)
                _statusBanner.value = msg
            }
        }
    }

    fun startVoiceCapture() {
        val currentProfile = (profileState.value as? UiDataState.Success)?.data
        val lang = HayatLanguage.fromCode(currentProfile?.preferredLanguage)
        voiceController.startListening(
            localeTag = lang.sttLocale,
            onResult = { spokenText ->
                sendPrompt(spokenText)
            },
            onError = { errMsg ->
                _statusBanner.value = errMsg
            }
        )
    }

    fun stopVoiceCapture() {
        voiceController.stopListening()
    }

    fun stopSpeaking() {
        voiceController.stopSpeaking()
    }

    fun speakMessage(text: String, languageCode: String = "en") {
        voiceController.speak(text, languageCode)
    }

    fun sendPrompt(rawPrompt: String, attachedImage: Bitmap? = null) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() && attachedImage == null) return
        val effectivePrompt = if (prompt.isEmpty()) {
            "Please describe this photo and help me with anything interesting or useful in it."
        } else {
            prompt
        }

        val currentProfile = (profileState.value as? UiDataState.Success)?.data
        val history = (messagesState.value as? UiDataState.Success)?.data.orEmpty()
        val langGuess = if (effectivePrompt.any { it in '\u0900'..'\u097F' }) "hi" else "en"

        viewModelScope.launch {
            _isThinking.value = true
            _apiStatus.value = ApiConnectionStatus.Connecting
            val startedAt = System.currentTimeMillis()

            repository.addMessage(
                userId = currentUserId,
                role = "user",
                text = effectivePrompt,
                language = langGuess,
                hasImage = attachedImage != null
            )

            try {
                val notificationsSummary = NotificationCache.summarizeForPrompt()
                val aiResult = aiEngine.generateAssistantReply(
                    userPrompt = effectivePrompt,
                    profile = currentProfile,
                    recentHistory = history,
                    attachedImage = attachedImage,
                    recentNotificationsText = notificationsSummary
                )

                val latency = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                _apiStatus.value = ApiConnectionStatus.Connected(latency)

                val actionTag = when {
                    attachedImage != null && aiResult.actionType == HayatActionType.NONE -> "PHOTO_ANALYSIS"
                    aiResult.actionType != HayatActionType.NONE -> aiResult.actionType.name
                    else -> null
                }

                repository.addMessage(
                    userId = currentUserId,
                    role = "model",
                    text = aiResult.replyText,
                    language = aiResult.detectedLanguage,
                    actionTag = actionTag,
                    hasImage = false
                )

                if (aiResult.memoryUpdate.isNotBlank()) {
                    val combinedMemory = buildString {
                        val existing = currentProfile?.memorySummary.orEmpty().trim()
                        if (existing.isNotEmpty() && !existing.contains(aiResult.memoryUpdate, ignoreCase = true)) {
                            append(existing)
                            append(" • ")
                        }
                        append(aiResult.memoryUpdate)
                    }.take(1000)
                    repository.updateProfileFields(userId = currentUserId, memorySummary = combinedMemory)
                }

                if (aiResult.actionType == HayatActionType.SAVE_REMINDER && aiResult.actionTarget.isNotBlank()) {
                    repository.addReminder(
                        userId = currentUserId,
                        title = aiResult.actionTarget,
                        timeLabel = "AI Reminder",
                        category = "Reminder"
                    )
                } else if (aiResult.actionType != HayatActionType.NONE) {
                    _actionEvents.tryEmit(
                        DeviceActionEvent(aiResult.actionType, aiResult.actionTarget)
                    )
                }

                if (currentProfile?.voiceEnabled != false) {
                    voiceController.speak(aiResult.replyText, aiResult.detectedLanguage)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "HAYAT could not reach Gemini right now."
                _apiStatus.value = ApiConnectionStatus.Error(errMsg)
                _statusBanner.value = errMsg
                Log.e(TAG, "sendPrompt failed", e)
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun updateMood(mood: HayatMood) {
        viewModelScope.launch {
            repository.updateProfileFields(userId = currentUserId, mood = mood.label)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not save mood." }
        }
    }

    fun updateLanguage(language: HayatLanguage) {
        viewModelScope.launch {
            repository.updateProfileFields(userId = currentUserId, preferredLanguage = language.code)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not save language." }
        }
    }

    fun updateProfileSettings(
        displayName: String? = null,
        voiceEnabled: Boolean? = null,
        ttsSpeechRate: Double? = null,
        backgroundAssistantEnabled: Boolean? = null,
        cityName: String? = null,
        memorySummary: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfileFields(
                userId = currentUserId,
                displayName = displayName,
                voiceEnabled = voiceEnabled,
                ttsSpeechRate = ttsSpeechRate,
                backgroundAssistantEnabled = backgroundAssistantEnabled,
                cityName = cityName,
                memorySummary = memorySummary
            ).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to update settings."
            }
        }
    }

    fun addReminder(title: String, timeLabel: String, category: String) {
        viewModelScope.launch {
            repository.addReminder(currentUserId, title, timeLabel, category)
                .onSuccess { _statusBanner.value = "Saved \"$title\"" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to save reminder" }
        }
    }

    fun toggleReminder(reminderId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleReminder(currentUserId, reminderId, completed)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update reminder" }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            repository.deleteReminder(currentUserId, reminderId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to delete reminder" }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearChatHistory(currentUserId)
                .onSuccess { _statusBanner.value = "Conversation history cleared" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to clear history" }
        }
    }

    override fun onCleared() {
        voiceController.release()
        super.onCleared()
    }
}
