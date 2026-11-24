package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HayatLanguage
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.RosePulse

@Composable
fun MicControlBar(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    voiceLevel: Float,
    partialTranscript: String,
    selectedLanguage: HayatLanguage,
    onLanguageSelected: (HayatLanguage) -> Unit,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSendText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draftText by rememberSaveable { mutableStateOf("") }

    val micPulse = if (isListening) {
        val transition = rememberInfiniteTransition(label = "mic_pulse")
        val s by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "mic_ring"
        )
        s
    } else {
        1.0f
    }
    val levelScale by animateFloatAsState(
        targetValue = if (isListening) 1f + (voiceLevel * 0.2f) else 1f,
        animationSpec = tween(100),
        label = "mic_rms"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status or live transcript feedback
        val caption = when {
            isListening && partialTranscript.isNotBlank() -> "\"$partialTranscript\""
            isListening -> stringResource(R.string.mic_listening)
            isThinking -> stringResource(R.string.mic_thinking)
            isSpeaking -> stringResource(R.string.mic_speaking)
            else -> stringResource(R.string.mic_tap_to_speak)
        }

        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isListening) RosePulse else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Language selector + Central Voice Mic + Stop Speaking control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language pill cycler (EN / हिंदी / Hinglish)
            Surface(
                shape = RoundedCornerShape(50),
                color = CyberSlate,
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("language_cycle_button")
                    .clickable {
                        val next = when (selectedLanguage) {
                            HayatLanguage.HINGLISH -> HayatLanguage.HINDI
                            HayatLanguage.HINDI -> HayatLanguage.ENGLISH
                            HayatLanguage.ENGLISH -> HayatLanguage.HINGLISH
                        }
                        onLanguageSelected(next)
                    }
            ) {
                Text(
                    text = selectedLanguage.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Glowing Microphone Button with Voice Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp)
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(micPulse * levelScale)
                            .clip(CircleShape)
                            .background(RosePulse.copy(alpha = 0.25f))
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = ObsidianVoid,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isListening) {
                                    listOf(RosePulse, AuroraViolet)
                                } else {
                                    listOf(CyberCyan, AuroraViolet)
                                }
                            )
                        )
                        .testTag("voice_mic_button")
                        .clickable(onClick = onMicClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                            contentDescription = if (isListening) "Stop listening" else "Start voice command",
                            tint = ObsidianVoid,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            // Stop TTS / Mute button
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSpeaking) RosePulse.copy(alpha = 0.2f) else CyberSlate,
                border = BorderStroke(1.dp, if (isSpeaking) RosePulse else OutlineSubtle),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("stop_tts_button")
                    .clickable(enabled = isSpeaking, onClick = onStopSpeaking)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop HAYAT voice",
                        tint = if (isSpeaking) RosePulse else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mute",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSpeaking) RosePulse else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact text command bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                placeholder = {
                    Text(
                        text = "Ask HAYAT in English or हिंदी...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MidnightGlass,
                    unfocusedContainerColor = MidnightGlass,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = OutlineSubtle
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (draftText.isNotBlank() && !isThinking) {
                            onSendText(draftText)
                            draftText = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_text_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (draftText.isNotBlank() && !isThinking) CyberCyan else CyberSlate,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        if (draftText.isNotBlank() && !isThinking) {
                            onSendText(draftText)
                            draftText = ""
                        }
                    },
                    enabled = draftText.isNotBlank() && !isThinking,
                    modifier = Modifier.testTag("send_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send command",
                        tint = if (draftText.isNotBlank() && !isThinking) ObsidianVoid else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
