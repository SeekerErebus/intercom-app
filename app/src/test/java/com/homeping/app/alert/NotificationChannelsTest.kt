package com.homeping.app.alert

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelsTest {

    @Test
    fun channelIds_matchDesignDoc() {
        assertEquals("service", NotificationChannels.SERVICE)
        assertEquals("ping_alerts", NotificationChannels.PING_ALERTS)
    }

    @Test
    fun notificationIds_areStable() {
        assertEquals(1001, HomePingNotifications.SERVICE_NOTIFICATION_ID)
        assertEquals(1002, HomePingNotifications.PING_NOTIFICATION_ID)
    }
}
