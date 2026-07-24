package com.github.jimmy90109.livestatus.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusSnapshotTest {
    @Test
    fun requiredSettingsComplete_requiresBothNotificationSettings() {
        assertFalse(StatusSnapshot().requiredSettingsComplete)
        assertFalse(
            StatusSnapshot(
                notificationAccess = true,
                notificationPermission = false,
            ).requiredSettingsComplete,
        )
        assertFalse(
            StatusSnapshot(
                notificationAccess = false,
                notificationPermission = true,
            ).requiredSettingsComplete,
        )
        assertTrue(
            StatusSnapshot(
                notificationAccess = true,
                notificationPermission = true,
            ).requiredSettingsComplete,
        )
    }

    @Test
    fun requiredSettingsComplete_doesNotChangeAppPreferencesOrInstallState() {
        val status = StatusSnapshot(
            notificationAccess = false,
            notificationPermission = false,
            clockInstalled = true,
            clockEnabled = true,
            ipassInstalled = false,
            ipassEnabled = false,
        )

        assertFalse(status.requiredSettingsComplete)
        assertTrue(status.clockInstalled)
        assertTrue(status.clockEnabled)
        assertFalse(status.ipassInstalled)
        assertFalse(status.ipassEnabled)
    }
}
