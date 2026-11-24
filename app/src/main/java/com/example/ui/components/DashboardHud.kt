package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HayatMood
import com.example.model.WeatherState
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.RosePulse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GreetingHeader(
    userName: String,
    isBgServiceRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 5..11 -> "Good Morning"
            hour in 12..16 -> "Good Afternoon"
            hour in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HAYAT AI",
                style = MaterialTheme.typography.labelMedium,
                color = CyberCyan
            )
            Text(
                text = "$greeting, $userName",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = if (isBgServiceRunning) EmeraldOnline.copy(alpha = 0.14f) else CyberSlate,
            border = BorderStroke(
                1.dp,
                if (isBgServiceRunning) EmeraldOnline.copy(alpha = 0.55f) else OutlineSubtle
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = if (isBgServiceRunning) EmeraldOnline else CyberCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isBgServiceRunning) "BG LIVE" else "GEMINI",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBgServiceRunning) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeatherDateMoodCard(
    weather: WeatherState,
    selectedMood: HayatMood,
    onMoodSelected: (HayatMood) -> Unit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MidnightGlass,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CyberSlate,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = "Weather status",
                            tint = AmberWarm,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val tempText = weather.temperatureC?.let { "${it.toInt()}°C" } ?: "--°C"
                        Text(
                            text = "$tempText · ${weather.conditionLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${weather.city} · $formattedDate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshWeather,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("refresh_weather_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh live weather",
                        tint = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOOD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HayatMood.entries.forEach { mood ->
                    val selected = mood == selectedMood
                    FilterChip(
                        selected = selected,
                        onClick = { onMoodSelected(mood) },
                        label = {
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            containerColor = CyberSlate,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = OutlineSubtle,
                            selectedBorderColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("mood_chip_${mood.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFeatureBar(
    onMusicClick: () -> Unit,
    onStudyClick: () -> Unit,
    onChatClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureTile(
            label = stringResource(R.string.action_music),
            icon = Icons.Filled.MusicNote,
            accent = RosePulse,
            onClick = onMusicClick,
            testTag = "feature_music_button",
            modifier = Modifier.weight(1f)
        )
        FeatureTile(
            label = stringResource(R.string.action_study),
            icon = Icons.Filled.School,
            accent = AuroraViolet,
            onClick = onStudyClick,
            testTag = "feature_study_button",
            modifier = Modifier.weight(1f)
        )
        FeatureTile(
            label = stringResource(R.string.action_chat),
            icon = Icons.Filled.ChatBubble,
            accent = CyberCyan,
            onClick = onChatClick,
            testTag = "feature_chat_button",
            modifier = Modifier.weight(1f)
        )
        FeatureTile(
            label = stringResource(R.string.action_photo),
            icon = Icons.Filled.PhotoCamera,
            accent = EmeraldOnline,
            onClick = onPhotoClick,
            testTag = "feature_photo_button",
            modifier = Modifier.weight(1f)
        )
        FeatureTile(
            label = stringResource(R.string.action_settings),
            icon = Icons.Filled.Settings,
            accent = AmberWarm,
            onClick = onSettingsClick,
            testTag = "feature_settings_button",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CyberSlate,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
