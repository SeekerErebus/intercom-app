package com.homeping.app.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

/**
 * Shows/clears high-priority ping notifications and plays a strong default sound.
 */
class PingAlerter(context: Context) {
    private val appContext = context.applicationContext
    private var ringtone: android.media.Ringtone? = null

    fun showIncoming(pingId: String, fromName: String) {
        NotificationChannels.ensureCreated(appContext)
        val notification = HomePingNotifications.incomingPing(appContext, fromName, pingId)
        NotificationManagerCompat.from(appContext)
            .notify(HomePingNotifications.PING_NOTIFICATION_ID, notification)
        playAlertSound()
        vibrate()
    }

    fun clear(pingId: String? = null) {
        NotificationManagerCompat.from(appContext)
            .cancel(HomePingNotifications.PING_NOTIFICATION_ID)
        stopAlertSound()
    }

    private fun playAlertSound() {
        stopAlertSound()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val rt = RingtoneManager.getRingtone(appContext, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rt.isLooping = true
            }
            rt.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone = rt
            rt.play()
        } catch (_: Exception) {
        }
    }

    private fun stopAlertSound() {
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
    }

    private fun vibrate() {
        try {
            val pattern = longArrayOf(0, 400, 200, 400, 200, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService<VibratorManager>() ?: return
                manager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 0),
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun stopVibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appContext.getSystemService<VibratorManager>()?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                (appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
            }
        } catch (_: Exception) {
        }
    }

    fun clearAll() {
        clear()
        stopVibrate()
    }
}
