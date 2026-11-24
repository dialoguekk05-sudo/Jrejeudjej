package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.RosePulse

@Composable
fun HayatAvatarOrb(
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    voiceLevel: Float,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 156.dp
) {
    // Low-power animation: only run infinite pulse while actively speaking, listening, or thinking.
    val isActive = isSpeaking || isListening || isThinking
    val pulseScale = if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "hayat_orb_pulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = if (isSpeaking) 1.07f else 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isSpeaking) 420 else 750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_scale"
        )
        animated
    } else {
        1.0f
    }

    val reactiveBoost by animateFloatAsState(
        targetValue = if (isListening) (voiceLevel * 0.10f) else 0f,
        animationSpec = tween(120),
        label = "voice_boost"
    )

    val ringColor = when {
        isListening -> RosePulse
        isSpeaking -> CyberCyan
        isThinking -> AuroraViolet
        else -> CyberCyan.copy(alpha = 0.6f)
    }

    val statusText = when {
        isListening -> "LISTENING"
        isThinking -> "THINKING"
        isSpeaking -> "SPEAKING"
        else -> "ONLINE"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(avatarSize + 24.dp)
        ) {
            // Outer energy halo
            Box(
                modifier = Modifier
                    .size(avatarSize + 14.dp)
                    .scale(pulseScale + reactiveBoost)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ringColor.copy(alpha = if (isActive) 0.42f else 0.18f),
                                ringColor.copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            // Crisp neon ring border
            Box(
                modifier = Modifier
                    .size(avatarSize + 6.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(CyberCyan, AuroraViolet, ringColor)
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                Crossfade(
                    targetState = isSpeaking,
                    animationSpec = tween(durationMillis = 180),
                    label = "hayat_talking_face"
                ) { talking ->
                    Image(
                        painter = painterResource(
                            id = if (talking) R.drawable.img_hayat_speaking else R.drawable.img_hayat_avatar
                        ),
                        contentDescription = if (talking) {
                            "HAYAT AI anime assistant speaking"
                        } else {
                            "HAYAT AI anime assistant avatar"
                        },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            // Status pill anchored at bottom of avatar
            Surface(
                shape = RoundedCornerShape(50),
                color = MidnightGlass.copy(alpha = 0.94f),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .border(1.dp, ringColor.copy(alpha = 0.7f), RoundedCornerShape(50))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isActive) ringColor else EmeraldOnline)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
