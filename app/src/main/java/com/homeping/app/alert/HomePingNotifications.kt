package com.homeping.app.alert

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.homeping.app.MainActivity
import com.homeping.app.R

object HomePingNotifications {
    const val SERVICE_NOTIFICATION_ID = 1001
    const val PING_NOTIFICATION_ID = 1002

    fun serviceReady(context: Context): Notification {
        val openApp = activityPendingIntent(context, requestCode = 1)
        return NotificationCompat.Builder(context, NotificationChannels.SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_service_title))
            .setContentText(context.getString(R.string.notif_service_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Scaffold for incoming pings (used once transport/ping PRs land).
     */
    fun incomingPing(
        context: Context,
        fromName: String,
        pingId: String,
    ): Notification {
        val openApp = activityPendingIntent(
            context,
            requestCode = 2,
            extras = mapOf(EXTRA_PING_ID to pingId),
        )
        return NotificationCompat.Builder(context, NotificationChannels.PING_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_ping_title, fromName))
            .setContentText(context.getString(R.string.notif_ping_text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                context.getString(R.string.notif_ping_coming),
                // Actions wired to receivers in the ping PR.
                activityPendingIntent(context, requestCode = 3, extras = mapOf(EXTRA_PING_ID to pingId)),
            )
            .addAction(
                0,
                context.getString(R.string.notif_ping_dismiss),
                activityPendingIntent(context, requestCode = 4, extras = mapOf(EXTRA_PING_ID to pingId)),
            )
            .build()
    }

    const val EXTRA_PING_ID = "ping_id"

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        extras: Map<String, String> = emptyMap(),
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            extras.forEach { (k, v) -> putExtra(k, v) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
