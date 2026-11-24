package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.actions.DeviceActionExecutor
import com.example.model.AssistantReminder
import com.example.model.HayatActionType
import com.example.model.InstalledAppItem
import com.example.model.NotificationItem
import com.example.model.UiDataState
import com.example.service.NotificationCache
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.RosePulse

@Composable
fun ActionsScreen(
    remindersState: UiDataState<List<AssistantReminder>>,
    notifications: List<NotificationItem>,
    installedApps: List<InstalledAppItem>,
    onDeviceAction: (HayatActionType, String) -> Unit,
    onAddReminder: (String, String, String) -> Unit,
    onToggleReminder: (String, Boolean) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onReadNotificationsAloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasNotificationAccess = NotificationCache.isNotificationAccessEnabled(context)

    var phoneInput by rememberSaveable { mutableStateOf("") }
    var alarmTimeInput by rememberSaveable { mutableStateOf("07:00") }
    var reminderTitleInput by rememberSaveable { mutableStateOf("") }
    var appFilter by rememberSaveable { mutableStateOf("") }

    val filteredApps = remember(installedApps, appFilter) {
        if (appFilter.isBlank()) installedApps.take(12)
        else installedApps.filter { it.appName.contains(appFilter, ignoreCase = true) }.take(12)
    }

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
                    text = "PHONE COMMAND HUB",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan
                )
                Text(
                    text = "Apps, Calls, Alarms & Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. Essential App Launchers (YouTube, Instagram, WhatsApp, Music)
            SectionCard(title = "Quick App Launch") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPill(
                        label = "YouTube",
                        icon = Icons.Filled.PlayArrow,
                        accent = RosePulse,
                        onClick = { onDeviceAction(HayatActionType.OPEN_YOUTUBE, "") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPill(
                        label = "Instagram",
                        icon = Icons.Filled.Apps,
                        accent = AuroraViolet,
                        onClick = { onDeviceAction(HayatActionType.OPEN_INSTAGRAM, "") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPill(
                        label = "WhatsApp",
                        icon = Icons.Filled.Chat,
                        accent = EmeraldOnline,
                        onClick = { onDeviceAction(HayatActionType.OPEN_WHATSAPP, "") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPill(
                        label = "Music",
                        icon = Icons.Filled.MusicNote,
                        accent = CyberCyan,
                        onClick = { onDeviceAction(HayatActionType.PLAY_MUSIC, "") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Call Dialer via approved ACTION_DIAL intent
            SectionCard(title = "Phone Call (Approved Dialer)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        placeholder = { Text("Enter number (optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialer_number_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onDeviceAction(HayatActionType.MAKE_CALL, phoneInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("open_dialer_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Open Phone Dialer")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dial")
                    }
                }
            }

            // 3. Alarms & Cloud Reminders
            SectionCard(title = "Alarms & Cloud Reminders") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = reminderTitleInput,
                        onValueChange = { reminderTitleInput = it },
                        placeholder = { Text("Reminder or alarm label") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reminder_title_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = alarmTimeInput,
                        onValueChange = { alarmTimeInput = it },
                        placeholder = { Text("07:00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .width(96.dp)
                            .testTag("alarm_time_input")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val label = reminderTitleInput.ifBlank { "HAYAT Alarm" }
                            onDeviceAction(HayatActionType.SET_ALARM, "$alarmTimeInput|$label")
                            onAddReminder(label, alarmTimeInput, "Alarm")
                            reminderTitleInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_alarm_button")
                    ) {
                        Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Alarm")
                    }
                    OutlinedButton(
                        onClick = {
                            if (reminderTitleInput.isNotBlank()) {
                                onAddReminder(reminderTitleInput, alarmTimeInput, "Reminder")
                                reminderTitleInput = ""
                            }
                        },
                        enabled = reminderTitleInput.isNotBlank(),
                        border = BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_reminder_button")
                    ) {
                        Text("Save Note", color = CyberCyan)
                    }
                }

                val reminders = (remindersState as? UiDataState.Success)?.data.orEmpty()
                if (reminders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    reminders.take(6).forEach { rem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleReminder(rem.id, !rem.completed) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (rem.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "Toggle reminder completion",
                                    tint = if (rem.completed) EmeraldOnline else CyberCyan
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rem.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (rem.completed) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${rem.category} · ${rem.timeLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteReminder(rem.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete reminder",
                                    tint = RosePulse
                                )
                            }
                        }
                    }
                }
            }

            // 4. Notification Reader (only with Notification Access permission)
            SectionCard(title = "Notification Reader") {
                if (!hasNotificationAccess) {
                    Text(
                        text = "Grant Android Notification Access so HAYAT can read incoming alerts aloud only when you ask.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { NotificationCache.openNotificationAccessSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_notification_access_button")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Notification Access")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${notifications.size} recent alerts buffered",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldOnline
                        )
                        Button(
                            onClick = onReadNotificationsAloud,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = ObsidianVoid),
                            modifier = Modifier.testTag("read_notifications_button")
                        ) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Aloud")
                        }
                    }
                    if (notifications.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        notifications.take(4).forEach { n ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyberSlate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${n.appName} · ${n.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = n.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Supported Phone Settings Controls
            SectionCard(title = "Phone Settings Shortcuts") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingChip("Wi-Fi", Icons.Filled.Wifi) {
                        onDeviceAction(HayatActionType.OPEN_SETTINGS, "wifi")
                    }
                    SettingChip("Bluetooth", Icons.Filled.Bluetooth) {
                        onDeviceAction(HayatActionType.OPEN_SETTINGS, "bluetooth")
                    }
                    SettingChip("Sound", Icons.Filled.VolumeUp) {
                        onDeviceAction(HayatActionType.OPEN_SETTINGS, "sound")
                    }
                    SettingChip("Battery", Icons.Filled.BatteryChargingFull) {
                        onDeviceAction(HayatActionType.OPEN_SETTINGS, "battery")
                    }
                    SettingChip("All Settings", Icons.Filled.Settings) {
                        onDeviceAction(HayatActionType.OPEN_SETTINGS, "main")
                    }
                }
            }

            // 6. Launch Installed Applications
            SectionCard(title = "Installed Applications (${installedApps.size})") {
                OutlinedTextField(
                    value = appFilter,
                    onValueChange = { appFilter = it },
                    placeholder = { Text("Search installed apps...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = OutlineSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("installed_apps_search")
                )
                Spacer(modifier = Modifier.height(8.dp))
                filteredApps.forEach { app ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .minimumInteractiveComponentSize()
                            .clickable {
                                DeviceActionExecutor.launchInstalledAppByName(context, app.appName)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "OPEN",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MidnightGlass,
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyberSlate,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CyberSlate,
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
