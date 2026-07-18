package com.homeping.app.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.homeping.app.net.PingResponse
import com.homeping.app.ping.PingHub

/**
 * Handles Coming / Dismiss actions from the high-priority ping notification.
 */
class PingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pingId = intent.getStringExtra(HomePingNotifications.EXTRA_PING_ID).orEmpty()
        if (pingId.isBlank()) {
            Log.w(TAG, "missing pingId for $action")
            return
        }
        when (action) {
            ACTION_COMING -> {
                Log.i(TAG, "Coming for $pingId")
                PingHub.respond(pingId, PingResponse.Coming)
            }
            ACTION_DISMISS -> {
                Log.i(TAG, "Dismiss for $pingId")
                PingHub.respond(pingId, PingResponse.Dismissed)
            }
        }
        // Clear notification immediately for responsiveness.
        PingAlerter(context).clearAll()
    }

    companion object {
        private const val TAG = "PingActionReceiver"
        const val ACTION_COMING = "com.homeping.app.action.PING_COMING"
        const val ACTION_DISMISS = "com.homeping.app.action.PING_DISMISS"
    }
}
