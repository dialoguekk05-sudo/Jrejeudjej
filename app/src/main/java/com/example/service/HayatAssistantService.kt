package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object HayatBackgroundBridge {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _backgroundCommands = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val backgroundCommands: SharedFlow<String> = _backgroundCommands.asSharedFlow()

    internal fun setRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    internal fun emitCommand(command: String) {
        _backgroundCommands.tryEmit(command)
    }
}

class HayatAssistantService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                HayatBackgroundBridge.setRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_LISTEN -> {
                HayatBackgroundBridge.emitCommand(CMD_LISTEN)
            }
            ACTION_STOP_SPEAKING -> {
                HayatBackgroundBridge.emitCommand(CMD_MUTE)
            }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        HayatBackgroundBridge.setRunning(true)
        return START_STICKY
    }

    override fun onDestroy() {
        HayatBackgroundBridge.setRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenPending = PendingIntent.getService(
            this,
            101,
            Intent(this, HayatAssistantService::class.java).apply { action = ACTION_LISTEN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mutePending = PendingIntent.getService(
            this,
            102,
            Intent(this, HayatAssistantService::class.java).apply { action = ACTION_STOP_SPEAKING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this,
            103,
            Intent(this, HayatAssistantService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("HAYAT AI Active")
            .setContentText("Ready in background for Hindi & English voice commands")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPending)
            .addAction(0, "Listen", listenPending)
            .addAction(0, "Mute", mutePending)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.bg_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.bg_service_channel_desc)
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "hayat_assistant_channel"
        const val NOTIFICATION_ID = 4101
        const val ACTION_START = "com.example.action.START_HAYAT"
        const val ACTION_STOP = "com.example.action.STOP_HAYAT"
        const val ACTION_LISTEN = "com.example.action.LISTEN_HAYAT"
        const val ACTION_STOP_SPEAKING = "com.example.action.STOP_SPEAKING_HAYAT"
        const val CMD_LISTEN = "CMD_LISTEN"
        const val CMD_MUTE = "CMD_MUTE"

        fun start(context: Context) {
            val intent = Intent(context, HayatAssistantService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HayatAssistantService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
