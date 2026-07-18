package com.homeping.app.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.content.getSystemService
import com.homeping.app.R

/**
 * Creates and owns HomePing notification channel IDs.
 * Safe to call repeatedly (create is idempotent for same id attributes on most APIs).
 */
object NotificationChannels {
    const val SERVICE = "service"
    const val PING_ALERTS = "ping_alerts"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        val serviceChannel = NotificationChannel(
            SERVICE,
            context.getString(R.string.channel_service_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_service_description)
            setShowBadge(false)
        }

        val pingChannel = NotificationChannel(
            PING_ALERTS,
            context.getString(R.string.channel_ping_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_ping_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
            setShowBadge(true)
            // Default notification sound until custom tones land in a later PR.
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                attrs,
            )
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(pingChannel)
    }
}
