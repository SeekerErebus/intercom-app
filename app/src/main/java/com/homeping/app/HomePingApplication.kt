package com.homeping.app

import android.app.Application
import com.homeping.app.alert.NotificationChannels

class HomePingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }
}
