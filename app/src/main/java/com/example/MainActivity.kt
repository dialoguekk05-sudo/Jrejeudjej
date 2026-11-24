package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.actions.DeviceActionExecutor
import com.example.core.attemptAutoSignIn
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.HayatAiEngine
import com.example.data.HayatFirestoreRepository
import com.example.data.WeatherRepository
import com.example.model.HayatActionType
import com.example.model.UiDataState
import com.example.service.HayatAssistantService
import com.example.service.HayatBackgroundBridge
import com.example.service.NotificationCache
import com.example.ui.HayatViewModel
import com.example.ui.screens.ActionsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.HayatAiTheme
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.ObsidianVoid
import com.example.voice.HayatVoiceController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class HayatTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    CHAT("Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    ACTIONS("Actions", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure Firebase App Check with launch intent BEFORE setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            HayatAiTheme {
                HayatRootApp()
            }
        }
    }
}

@Composable
fun HayatRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: "Friend"
        key(uid) {
            HayatAuthenticatedShell(
                currentUserId = uid,
                defaultDisplayName = displayName,
                userEmail = user.email,
                onSignOut = {
                    HayatAssistantService.stop(context)
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun HayatAuthenticatedShell(
    currentUserId: String,
    defaultDisplayName: String,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: HayatViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HayatViewModel(
                    repository = HayatFirestoreRepository(db),
                    aiEngine = HayatAiEngine(),
                    weatherRepository = WeatherRepository(),
                    voiceController = HayatVoiceController(app),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val apiStatus by viewModel.apiStatus.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val isListening by viewModel.voiceController.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceController.isSpeaking.collectAsStateWithLifecycle()
    val voiceLevel by viewModel.voiceController.voiceLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.voiceController.partialTranscript.collectAsStateWithLifecycle()

    val isBgServiceRunning by HayatBackgroundBridge.isServiceRunning.collectAsStateWithLifecycle()
    val notifications by NotificationCache.notifications.collectAsStateWithLifecycle()

    val profile = (profileState as? UiDataState.Success)?.data
    val messages = (messagesState as? UiDataState.Success)?.data.orEmpty()

    var activeTab by rememberSaveable { mutableStateOf(HayatTab.HOME) }
    var pendingPhoto by remember { mutableStateOf<Bitmap?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val installedApps = remember(context) {
        DeviceActionExecutor.queryInstalledApps(context)
    }

    // Back navigation returns from sub-tabs to Home cleanly
    BackHandler(enabled = activeTab != HayatTab.HOME) {
        activeTab = HayatTab.HOME
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = DeviceActionExecutor.decodeDownsampledBitmap(context, uri)
            if (bitmap != null) {
                pendingPhoto = bitmap
                activeTab = HayatTab.CHAT
                viewModel.showBanner("Photo attached! Ask HAYAT anything about it.")
            } else {
                viewModel.showBanner("Could not read selected photo.")
            }
        }
    }

    // Runtime permission launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceCapture()
        } else {
            viewModel.showBanner("Microphone permission is needed for voice commands.")
        }
    }

    // Runtime permission launcher for Background Foreground Service (Mic + Notifications)
    val bgPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micOk = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (micOk && notifOk) {
            HayatAssistantService.start(context)
            viewModel.updateProfileSettings(backgroundAssistantEnabled = true)
        } else {
            viewModel.showBanner("Microphone & notification permissions are required for background mode.")
        }
    }

    val handleMicTrigger: () -> Unit = {
        if (isListening) {
            viewModel.stopVoiceCapture()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.startVoiceCapture()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val handleDeviceAction: (HayatActionType, String) -> Unit = { actionType, target ->
        if (actionType == HayatActionType.READ_NOTIFICATIONS) {
            if (!NotificationCache.isNotificationAccessEnabled(context)) {
                NotificationCache.openNotificationAccessSettings(context)
                viewModel.showBanner("Enable HAYAT Notification Reader to read alerts.")
            }
        } else {
            val outcome = DeviceActionExecutor.executeAction(context, actionType, target)
            if (outcome.isNotBlank()) {
                viewModel.showBanner(outcome)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.actionEvents.collect { evt ->
            handleDeviceAction(evt.actionType, evt.target)
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianVoid,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MidnightGlass,
                contentColor = CyberCyan
            ) {
                HayatTab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberSlate,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                HayatTab.HOME -> HomeScreen(
                    profile = profile,
                    latestMessages = messages,
                    weather = weatherState,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isThinking = isThinking,
                    isBgServiceRunning = isBgServiceRunning,
                    voiceLevel = voiceLevel,
                    partialTranscript = partialTranscript,
                    onMoodSelected = viewModel::updateMood,
                    onLanguageSelected = viewModel::updateLanguage,
                    onRefreshWeather = { viewModel.refreshWeather() },
                    onMusicClick = {
                        val outcome = DeviceActionExecutor.playMusic(context, "")
                        viewModel.showBanner(outcome)
                    },
                    onStudyClick = {
                        activeTab = HayatTab.CHAT
                        viewModel.showBanner("Study Mode ready — ask any topic or attach a photo!")
                    },
                    onChatClick = { activeTab = HayatTab.CHAT },
                    onPhotoClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSettingsClick = { activeTab = HayatTab.SETTINGS },
                    onMicClick = handleMicTrigger,
                    onStopSpeaking = viewModel::stopSpeaking,
                    onReplayMessage = { msg -> viewModel.speakMessage(msg.text, msg.language) },
                    onSendText = { text -> viewModel.sendPrompt(text) }
                )

                HayatTab.CHAT -> ChatScreen(
                    messagesState = messagesState,
                    isThinking = isThinking,
                    isListening = isListening,
                    pendingPhoto = pendingPhoto,
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onClearPendingPhoto = { pendingPhoto = null },
                    onMicClick = handleMicTrigger,
                    onSendPrompt = { text, bmp ->
                        viewModel.sendPrompt(text, bmp)
                        pendingPhoto = null
                    },
                    onSpeakMessage = { msg -> viewModel.speakMessage(msg.text, msg.language) },
                    onClearHistory = viewModel::clearHistory
                )

                HayatTab.ACTIONS -> ActionsScreen(
                    remindersState = remindersState,
                    notifications = notifications,
                    installedApps = installedApps,
                    onDeviceAction = handleDeviceAction,
                    onAddReminder = viewModel::addReminder,
                    onToggleReminder = viewModel::toggleReminder,
                    onDeleteReminder = viewModel::deleteReminder,
                    onReadNotificationsAloud = {
                        val summary = NotificationCache.summarizeForPrompt()
                        viewModel.sendPrompt("Please summarize and read out my recent notifications:\n$summary")
                    }
                )

                HayatTab.SETTINGS -> SettingsScreen(
                    profile = profile,
                    userEmail = userEmail,
                    apiStatus = apiStatus,
                    isBgServiceRunning = isBgServiceRunning,
                    onLanguageSelected = viewModel::updateLanguage,
                    onSaveProfile = { name, city, memory, rate, voice ->
                        viewModel.updateProfileSettings(
                            displayName = name,
                            cityName = city,
                            memorySummary = memory,
                            ttsSpeechRate = rate,
                            voiceEnabled = voice
                        )
                        viewModel.showBanner("Profile & preferences saved.")
                    },
                    onToggleBackgroundService = { enable ->
                        if (enable) {
                            val needed = mutableListOf<String>()
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                needed.add(Manifest.permission.RECORD_AUDIO)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                needed.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (needed.isEmpty()) {
                                HayatAssistantService.start(context)
                                viewModel.updateProfileSettings(backgroundAssistantEnabled = true)
                            } else {
                                bgPermissionsLauncher.launch(needed.toTypedArray())
                            }
                        } else {
                            HayatAssistantService.stop(context)
                            viewModel.updateProfileSettings(backgroundAssistantEnabled = false)
                        }
                    },
                    onTestApiConnection = viewModel::testApiConnection,
                    onSignOut = onSignOut
                )
            }
        }
    }
}
