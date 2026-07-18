package com.homeping.app.service

import android.content.Context
import android.util.Log

/**
 * Starts or stops [HomePingService] according to setup + notification permission.
 */
object ServiceLifecycle {
    private const val TAG = "ServiceLifecycle"

    fun sync(context: Context, setupComplete: Boolean) {
        if (!setupComplete) {
            HomePingService.stop(context)
            return
        }
        if (!NotificationPermission.hasPostNotifications(context)) {
            Log.i(TAG, "setup complete but notification permission missing; not starting FGS")
            return
        }
        HomePingService.start(context)
    }
}
