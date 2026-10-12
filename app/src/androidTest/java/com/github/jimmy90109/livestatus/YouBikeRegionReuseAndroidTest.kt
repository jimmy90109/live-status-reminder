package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.github.jimmy90109.livestatus.ui.home.YouBikeRegionPickerActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class YouBikeRegionReuseAndroidTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val names = listOf("you_bike_region_preference", "you_bike_ride_session", "you_bike_feedback_prompts", "app_reminder_preferences")
    private lateinit var savedPreferences: Map<String, Map<String, *>>
    private val candidates = setOf(YouBikeRegion.NEW_TAIPEI, YouBikeRegion.TAICHUNG, YouBikeRegion.KAOHSIUNG)

    @Before
    fun prepare() {
        savedPreferences = names.associateWith { context.getSharedPreferences(it, Context.MODE_PRIVATE).all.toMap() }
        YouBikeRideManager.clear(context)
        names.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        AppReminderPreferences.App.YOUBIKE.setEnabled(context, true)
    }

    @After
    fun cleanup() {
        YouBikeRideManager.clear(context)
        notificationManager.cancel(109)
        savedPreferences.forEach { (name, values) ->
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Long -> editor.putLong(key, value)
                    is Int -> editor.putInt(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.commit()
        }
        YouBikeRideManager.restore(context)
    }

    @Test
    fun borrowReturnReuseCorrectionAndDuplicateKeepExpectedRegion() {
        borrow()
        val first = requireNotNull(YouBikeRideSessionStore.load(context))
        assertEquals(candidates, first.candidateRegions)
        assertEquals(YouBikeRegion.UNRESOLVED, first.region)
        assertTrue(YouBikeRideManager.selectRegion(context, first.id, YouBikeRegion.KAOHSIUNG))
        finish(first)
        assertNull(YouBikeRideSessionStore.load(context))
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
        borrow(bike = "0102752")
        val reused = requireNotNull(YouBikeRideSessionStore.load(context))
        assertTrue(reused.isRegionReused)
        assertNull(reused.manuallySelectedRegion)
        assertEquals(YouBikeRegion.KAOHSIUNG, reused.region)
        if (notificationManager.areNotificationsEnabled()) {
            val notification = notificationManager.activeNotifications.single { it.id == 1008 }.notification
            assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("沿用上次選擇：高雄市"))
            assertEquals("更改服務區域", notification.actions.single().title.toString())
        }
        assertTrue(YouBikeRideManager.selectRegion(context, reused.id, YouBikeRegion.TAICHUNG))
        borrow(bike = reused.bikeNumber, borrowedAt = reused.borrowedAtMillis)
        YouBikeRideManager.restore(context)
        assertEquals(YouBikeRegion.TAICHUNG, YouBikeRideSessionStore.load(context)?.manuallySelectedRegion)
        finish(reused)
        borrow(bike = "0102753")
        assertEquals(YouBikeRegion.TAICHUNG, YouBikeRideSessionStore.load(context)?.region)
        assertTrue(YouBikeRideSessionStore.load(context)?.isRegionReused == true)
    }

    @Test
    fun storeRoundTripLegacyDefaultsAndNoBackfill() {
        val session = session().copy(isRegionReused = true, isSimulation = true)
        YouBikeRideSessionStore.save(context, session)
        assertEquals(session, YouBikeRideSessionStore.load(context))
        context.getSharedPreferences("you_bike_ride_session", Context.MODE_PRIVATE).edit()
            .remove("region_reused").remove("simulation").putString("manual_region", "KAOHSIUNG").commit()
        val legacy = requireNotNull(YouBikeRideSessionStore.load(context))
        assertFalse(legacy.isRegionReused)
        assertFalse(legacy.isSimulation)
        YouBikeRideManager.restore(context)
        assertNull(YouBikeRegionPreferenceStore.load(context))
    }

    @Test
    fun disabledFeatureClearsOnlyRideAndOtherClearsPreference() {
        YouBikeRegionPreferenceStore.save(context, YouBikeRegion.KAOHSIUNG)
        YouBikeRideSessionStore.save(context, session())
        AppReminderPreferences.App.YOUBIKE.setEnabled(context, false)
        YouBikeRideManager.restore(context)
        assertNull(YouBikeRideSessionStore.load(context))
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
        AppReminderPreferences.App.YOUBIKE.setEnabled(context, true)
        borrow()
        val ride = requireNotNull(YouBikeRideSessionStore.load(context))
        assertTrue(YouBikeRideManager.selectRegion(context, ride.id, YouBikeRegion.UNSUPPORTED))
        assertNull(YouBikeRegionPreferenceStore.load(context))
        assertFalse(YouBikeRideSessionStore.load(context)?.isRegionReused == true)
    }

    @Test
    fun staleExpiredAndInvalidSelectionsDoNotWritePreference() {
        YouBikeRegionPreferenceStore.save(context, YouBikeRegion.KAOHSIUNG)
        val ride = session()
        YouBikeRideSessionStore.save(context, ride)
        assertFalse(YouBikeRideManager.selectRegion(context, "stale", YouBikeRegion.TAICHUNG))
        assertFalse(YouBikeRideManager.selectRegion(context, ride.id, YouBikeRegion.TAIPEI))
        YouBikeRideSessionStore.save(context, ride.copy(borrowedAtMillis = System.currentTimeMillis() - 86_400_000L))
        assertFalse(YouBikeRideManager.selectRegion(context, ride.id, YouBikeRegion.TAICHUNG))
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
    }

    @Test
    fun simulationCanReuseButNeverChangesPreference() {
        YouBikeRegionPreferenceStore.save(context, YouBikeRegion.KAOHSIUNG)
        borrow(isSimulation = true)
        val ride = requireNotNull(YouBikeRideSessionStore.load(context))
        assertTrue(ride.isSimulation)
        assertTrue(ride.isRegionReused)
        assertTrue(YouBikeRideManager.selectRegion(context, ride.id, YouBikeRegion.TAICHUNG))
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
        YouBikeRideManager.restore(context)
        assertTrue(YouBikeRideManager.selectRegion(context, ride.id, YouBikeRegion.UNSUPPORTED))
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
    }

    @Test
    fun invalidSavedRegionIsIgnoredAndUniqueOrUnknownBorrowNeverChangesPreference() {
        val preferences = context.getSharedPreferences("you_bike_region_preference", Context.MODE_PRIVATE)
        for (value in listOf("invalid", "UNSUPPORTED", "UNRESOLVED")) {
            preferences.edit().putString("last_manual_region", value).commit()
            assertNull(YouBikeRegionPreferenceStore.load(context))
        }
        YouBikeRegionPreferenceStore.save(context, YouBikeRegion.KAOHSIUNG)
        borrow(station = "龍江錦州街口")
        assertEquals(YouBikeRegion.TAIPEI, YouBikeRideSessionStore.load(context)?.region)
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
        YouBikeRideManager.clear(context)
        borrow(station = "測試未知站")
        assertEquals(YouBikeRegion.UNRESOLVED, YouBikeRideSessionStore.load(context)?.region)
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
    }

    @Test
    fun pickerShowsCurrentSelectionAndCancelKeepsPreference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        YouBikeRegionPreferenceStore.save(context, YouBikeRegion.KAOHSIUNG)
        val ride = session().copy(isRegionReused = true)
        YouBikeRideSessionStore.save(context, ride)
        val activity = instrumentation.startActivitySync(YouBikeRegionPickerActivity.createIntent(context, ride.id))
        try {
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.waitForIdle(500, 5_000)
            val root = requireNotNull(instrumentation.uiAutomation.rootInActiveWindow)
            val current = findNode(root, "高雄市（目前選擇）")
            assertNotNull("Current region must be visible", current)
            assertTrue(current!!.isSelected || current.isChecked)
            var cancel = requireNotNull(findNode(root, "取消"))
            while (!cancel.isClickable && cancel.parent != null) cancel = cancel.parent
            assertTrue(cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            instrumentation.waitForIdleSync()
            assertEquals(ride, YouBikeRideSessionStore.load(context))
            assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionPreferenceStore.load(context))
        } finally {
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
        }
    }

    private fun findNode(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString() == text) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            findNode(child, text)?.let { return it }
        }
        return null
    }

    private fun session() = YouBikeRideSession(
        id = "test-ride", borrowedAtMillis = System.currentTimeMillis() - 60_000,
        stationName = "建國國小", dockNumber = "01", bikeNumber = "0102751",
        region = YouBikeRegion.KAOHSIUNG, candidateRegions = candidates,
        originalResolutionIssue = YouBikeStationResolutionIssue.AMBIGUOUS,
    )

    private fun borrow(station: String = "建國國小", bike: String = "0102751", borrowedAt: Long = System.currentTimeMillis() - 10_000, isSimulation: Boolean = false) {
        YouBikeRideManager.handle(context, YouBikeRideUpdate(
            event = YouBikeEvent.BORROWED, occurredAt = timestamp(borrowedAt),
            stationName = station, dockNumber = "01", bikeNumber = bike,
        ), isSimulation = isSimulation)
    }

    private fun finish(ride: YouBikeRideSession) {
        YouBikeRideManager.handle(context, YouBikeRideUpdate(
            event = YouBikeEvent.RETURNED, occurredAt = timestamp(System.currentTimeMillis()), bikeNumber = ride.bikeNumber,
        ))
    }

    private fun timestamp(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("Asia/Taipei"))
}
