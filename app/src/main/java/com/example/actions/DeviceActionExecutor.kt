package com.example.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.model.HayatActionType
import com.example.model.InstalledAppItem
import java.net.URLEncoder

object DeviceActionExecutor {

    fun executeAction(
        context: Context,
        actionType: HayatActionType,
        target: String
    ): String {
        return when (actionType) {
            HayatActionType.NONE -> ""
            HayatActionType.OPEN_YOUTUBE -> openYouTube(context, target)
            HayatActionType.OPEN_INSTAGRAM -> launchPackageOrWeb(
                context,
                "com.instagram.android",
                "https://www.instagram.com/",
                "Instagram"
            )
            HayatActionType.OPEN_WHATSAPP -> launchPackageOrWeb(
                context,
                "com.whatsapp",
                "https://web.whatsapp.com/",
                "WhatsApp"
            )
            HayatActionType.PLAY_MUSIC -> playMusic(context, target)
            HayatActionType.LAUNCH_APP -> launchInstalledAppByName(context, target)
            HayatActionType.MAKE_CALL -> openDialer(context, target)
            HayatActionType.SET_ALARM -> setSystemAlarm(context, target)
            HayatActionType.SET_TIMER -> setSystemTimer(context, target)
            HayatActionType.OPEN_SETTINGS -> openPhoneSetting(context, target)
            HayatActionType.READ_NOTIFICATIONS -> ""
            HayatActionType.SAVE_REMINDER -> "Saved reminder"
        }
    }

    fun openYouTube(context: Context, query: String): String {
        val cleanQuery = query.trim()
        val intent = if (cleanQuery.isNotBlank()) {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
        } else {
            context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/"))
        }
        return launchSafe(context, intent, "YouTube")
    }

    fun playMusic(context: Context, query: String): String {
        val cleanQuery = query.trim()
        if (cleanQuery.isNotBlank()) {
            val searchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                putExtra(android.app.SearchManager.QUERY, cleanQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                return launchSafe(context, searchIntent, "Music search: $cleanQuery")
            }
            return openYouTube(context, "$cleanQuery music")
        }

        val musicPackages = listOf(
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.google.android.music"
        )
        for (pkg in musicPackages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                return launchSafe(context, intent, "Music Player")
            }
        }
        val selector = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
        return launchSafe(context, selector, "Music")
    }

    fun openDialer(context: Context, phoneOrQuery: String): String {
        val digits = phoneOrQuery.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        val uri = if (digits.isNotBlank()) Uri.parse("tel:$digits") else Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        return launchSafe(context, intent, if (digits.isNotBlank()) "Dialer ($digits)" else "Phone Dialer")
    }

    fun setSystemAlarm(context: Context, target: String): String {
        val parts = target.split("|")
        val timePart = parts.getOrNull(0)?.trim().orEmpty()
        val label = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "HAYAT Alarm" }

        val timeSegments = timePart.split(":")
        val hour = timeSegments.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
        val minute = timeSegments.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return launchSafe(context, intent, "Alarm %02d:%02d".format(hour, minute))
    }

    fun setSystemTimer(context: Context, target: String): String {
        val clean = target.lowercase().trim()
        val numeric = clean.filter { it.isDigit() }.toIntOrNull() ?: 5
        val seconds = if (clean.endsWith("s")) numeric else numeric * 60
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(5))
            putExtra(AlarmClock.EXTRA_MESSAGE, "HAYAT Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return launchSafe(context, intent, "Timer (${seconds}s)")
    }

    fun openPhoneSetting(context: Context, settingKey: String): String {
        val action = when (settingKey.lowercase().trim()) {
            "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return launchSafe(context, Intent(action), "Phone Settings ($settingKey)")
    }

    fun launchInstalledAppByName(context: Context, query: String): String {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return "Specify an app name to launch."
        val apps = queryInstalledApps(context)
        val match = apps.firstOrNull { it.appName.lowercase() == clean }
            ?: apps.firstOrNull { it.appName.lowercase().contains(clean) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(clean) }

        return if (match != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(match.packageName)
            if (intent != null) {
                launchSafe(context, intent, match.appName)
            } else {
                "${match.appName} cannot be launched directly."
            }
        } else {
            "App \"$query\" is not installed on this device."
        }
    }

    fun launchPackageOrWeb(
        context: Context,
        packageName: String,
        fallbackUrl: String,
        label: String
    ): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchSafe(context, launchIntent, label)
        } else {
            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)), "$label (Web)")
        }
    }

    fun queryInstalledApps(context: Context): List<InstalledAppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val name = info.loadLabel(pm).toString().trim()
                if (name.isBlank()) return@mapNotNull null
                InstalledAppItem(appName = name, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun decodeDownsampledBitmap(context: Context, uri: Uri, maxDimensionPx: Int = 1024): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            while (maxSide / sampleSize > maxDimensionPx) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun launchSafe(context: Context, intent: Intent, label: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opened $label"
        } catch (_: Exception) {
            "Could not open $label on this device."
        }
    }
}
