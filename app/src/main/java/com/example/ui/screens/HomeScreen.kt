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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.HayatLanguage
import com.example.model.HayatMood
import com.example.model.UserProfile
import com.example.model.WeatherState
import com.example.ui.components.GreetingHeader
import com.example.ui.components.HayatAvatarOrb
import com.example.ui.components.MicControlBar
import com.example.ui.components.QuickFeatureBar
import com.example.ui.components.WeatherDateMoodCard
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.OutlineSubtle

@Composable
fun HomeScreen(
    profile: UserProfile?,
    latestMessages: List<ChatMessage>,
    weather: WeatherState,
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    isBgServiceRunning: Boolean,
    voiceLevel: Float,
    partialTranscript: String,
    onMoodSelected: (HayatMood) -> Unit,
    onLanguageSelected: (HayatLanguage) -> Unit,
    onRefreshWeather: () -> Unit,
    onMusicClick: () -> Unit,
    onStudyClick: () -> Unit,
    onChatClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit,
    onReplayMessage: (ChatMessage) -> Unit,
    onSendText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userName = profile?.displayName?.ifBlank { "Friend" } ?: "Friend"
    val selectedMood = HayatMood.fromLabel(profile?.mood)
    val selectedLang = HayatLanguage.fromCode(profile?.preferredLanguage)
    val latestReply = latestMessages.lastOrNull { it.role == "model" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GreetingHeader(
                userName = userName,
                isBgServiceRunning = isBgServiceRunning
            )

            Spacer(modifier = Modifier.height(10.dp))

            HayatAvatarOrb(
                isSpeaking = isSpeaking,
                isListening = isListening,
                isThinking = isThinking,
                voiceLevel = voiceLevel,
                avatarSize = 148.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            WeatherDateMoodCard(
                weather = weather,
                selectedMood = selectedMood,
                onMoodSelected = onMoodSelected,
                onRefreshWeather = onRefreshWeather
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickFeatureBar(
                onMusicClick = onMusicClick,
                onStudyClick = onStudyClick,
                onChatClick = onChatClick,
                onPhotoClick = onPhotoClick,
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Latest AI reply card or friendly welcome prompt
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MidnightGlass,
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.28f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (latestReply != null) "HAYAT RESPONSE" else "READY TO ASSIST",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }

                        if (latestReply != null) {
                            IconButton(
                                onClick = { onReplayMessage(latestReply) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("replay_latest_reply_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak HAYAT reply aloud",
                                    tint = CyberCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = latestReply?.text ?: "Namaste $userName! Main HAYAT hoon — tap the mic or type to chat in Hindi or English, open apps, analyze photos, or study together.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!latestReply?.actionTag.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CyberSlate,
                            border = BorderStroke(1.dp, AuroraViolet.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ACTION · ${latestReply?.actionTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraViolet,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MicControlBar(
                isListening = isListening,
                isThinking = isThinking,
                isSpeaking = isSpeaking,
                voiceLevel = voiceLevel,
                partialTranscript = partialTranscript,
                selectedLanguage = selectedLang,
                onLanguageSelected = onLanguageSelected,
                onMicClick = onMicClick,
                onStopSpeaking = onStopSpeaking,
                onSendText = onSendText
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
