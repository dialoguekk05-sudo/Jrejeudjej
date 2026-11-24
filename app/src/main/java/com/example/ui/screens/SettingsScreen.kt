package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ApiConnectionStatus
import com.example.model.HayatLanguage
import com.example.model.UserProfile
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.RosePulse

@Composable
fun SettingsScreen(
    profile: UserProfile?,
    userEmail: String?,
    apiStatus: ApiConnectionStatus,
    isBgServiceRunning: Boolean,
    onLanguageSelected: (HayatLanguage) -> Unit,
    onSaveProfile: (displayName: String, cityName: String, memorySummary: String, speechRate: Double, voiceEnabled: Boolean) -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onTestApiConnection: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName ?: "Friend")
    }
    var cityName by remember(profile?.cityName) {
        mutableStateOf(profile?.cityName ?: "New Delhi")
    }
    var memorySummary by remember(profile?.memorySummary) {
        mutableStateOf(profile?.memorySummary ?: "")
    }
    var voiceEnabled by remember(profile?.voiceEnabled) {
        mutableStateOf(profile?.voiceEnabled ?: true)
    }
    var speechRate by remember(profile?.ttsSpeechRate) {
        mutableFloatStateOf((profile?.ttsSpeechRate ?: 1.0).toFloat())
    }

    LaunchedEffect(profile) {
        if (profile != null) {
            displayName = profile.displayName
            cityName = profile.cityName
            memorySummary = profile.memorySummary
            voiceEnabled = profile.voiceEnabled
            speechRate = profile.ttsSpeechRate.toFloat()
        }
    }

    val currentLang = HayatLanguage.fromCode(profile?.preferredLanguage)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "PROFILE & SYSTEM",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan
                )
                Text(
                    text = "HAYAT Settings & Cloud Status",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. User Profile & Persistent Memory
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MidnightGlass,
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "User Profile & Memory",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(60) },
                        label = { Text("Your Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input")
                    )

                    OutlinedTextField(
                        value = cityName,
                        onValueChange = { cityName = it.take(80) },
                        label = { Text("City (for live weather)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_city_input")
                    )

                    OutlinedTextField(
                        value = memorySummary,
                        onValueChange = { memorySummary = it.take(1000) },
                        label = { Text("Assistant Memory Summary") },
                        placeholder = { Text("Facts HAYAT remembers about you...") },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_memory_input")
                    )

                    Button(
                        onClick = {
                            onSaveProfile(
                                displayName,
                                cityName,
                                memorySummary,
                                speechRate.toDouble(),
                                voiceEnabled
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ObsidianVoid
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile & Preferences")
                    }
                }
            }

            // 2. Language & Voice Controls
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MidnightGlass,
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "VOICE & LANGUAGE",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HayatLanguage.entries.forEach { lang ->
                            val selected = lang == currentLang
                            FilterChip(
                                selected = selected,
                                onClick = { onLanguageSelected(lang) },
                                label = { Text(lang.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan,
                                    containerColor = CyberSlate
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Speak Replies Aloud (TTS)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "HAYAT answers through phone speakers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { checked ->
                                voiceEnabled = checked
                                onSaveProfile(
                                    displayName,
                                    cityName,
                                    memorySummary,
                                    speechRate.toDouble(),
                                    checked
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = CyberCyan),
                            modifier = Modifier.testTag("voice_enabled_switch")
                        )
                    }

                    Text(
                        text = "Speech Rate: ${"%.1f".format(speechRate)}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                        modifier = Modifier.testTag("speech_rate_slider")
                    )
                }
            }

            // 3. Background Assistant Foreground Service
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MidnightGlass,
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Assistant Service",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Keep notification voice controls active when minimized or screen-locked (battery-safe).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isBgServiceRunning,
                        onCheckedChange = onToggleBackgroundService,
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = EmeraldOnline),
                        modifier = Modifier.testTag("background_service_switch")
                    )
                }
            }

            // 4. Backend & Gemini API Connection Status
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MidnightGlass,
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isErr = apiStatus is ApiConnectionStatus.Error
                        Icon(
                            imageVector = if (isErr) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = if (isErr) RosePulse else EmeraldOnline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Firebase AI Logic & App Check",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val statusDesc = when (apiStatus) {
                                ApiConnectionStatus.Ready -> "Configured securely (zero API keys in APK)"
                                ApiConnectionStatus.Connecting -> "Testing Gemini 3.8 Flash backend..."
                                is ApiConnectionStatus.Connected -> "Connected · ${apiStatus.latencyMs} ms"
                                is ApiConnectionStatus.Error -> "Error: ${apiStatus.message}"
                            }
                            Text(
                                text = statusDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isErr) RosePulse else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onTestApiConnection,
                        enabled = apiStatus !is ApiConnectionStatus.Connecting,
                        border = BorderStroke(1.dp, AuroraViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_api_connection_button")
                    ) {
                        if (apiStatus is ApiConnectionStatus.Connecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AuroraViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Verify Gemini Backend Connection", color = AuroraViolet)
                    }
                }
            }

            // 5. Sign Out
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, RosePulse.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = RosePulse)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = RosePulse)
            }
        }
    }
}
