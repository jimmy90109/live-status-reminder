package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jimmy90109.livestatus.ui.home.CitymapperSimulation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CitymapperNotificationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun extractionAndRenderingKeepSourceIntentsPrivacyAndOnlyAvailableActions() {
        val open = pendingIntent("OPEN")
        val end = pendingIntent("END")
        val next = pendingIntent("NEXT")
        for (visibility in listOf(Notification.VISIBILITY_PUBLIC, Notification.VISIBILITY_PRIVATE, Notification.VISIBILITY_SECRET)) {
            val source = Notification.Builder(context, "trip-progress")
                .setSmallIcon(R.drawable.ic_navigation_notification)
                .setContentTitle("Ride 7 stops to")
                .setContentText("示例站\nArrive 6:08 PM (127 min)")
                .setOngoing(true)
                .setVisibility(visibility)
                .setContentIntent(open)
                .addAction(Notification.Action.Builder(null, "End Trip", end).build())
                .addAction(Notification.Action.Builder(null, "Prev", null).build())
                .addAction(Notification.Action.Builder(null, "Next", next).build())
                .build()
            val update = requireNotNull(CitymapperNotificationExtractor.extract(context, source(source)))
            val rendered = LiveStatusReminder.buildCitymapperNotification(context, update)
            assertEquals(visibility, rendered.visibility)
            assertEquals(open, rendered.contentIntent)
            assertEquals(listOf("End Trip", "Next"), rendered.actions.map { it.title.toString() })
            assertEquals(listOf(end, next), rendered.actions.map { it.actionIntent })
            assertEquals("Ride 7 stops to", rendered.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals("示例站\n預計抵達時間：6:08 PM到達", rendered.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
            assertEquals("7站下車", rendered.shortCriticalText)
            assertEquals(R.drawable.ic_transit_notification, rendered.smallIcon.resId)
            assertTrue(rendered.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertTrue(rendered.extras.getBoolean("android.requestPromotedOngoing"))
            assertTrue(rendered.hasPromotableCharacteristics())
        }
    }

    @Test
    fun removesChineseAndEnglishSharingActionsButKeepsNavigationControls() {
        val notification = Notification.Builder(context, "trip-progress")
            .setSmallIcon(R.drawable.ic_navigation_notification)
            .setContentTitle("步行至")
            .setContentText("示例廣場\n下午6:21 (104分鐘)到達\n分享預計抵達時間")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "分享預計抵達時間", pendingIntent("SHARE_ZH")).build())
            .addAction(Notification.Action.Builder(null, "Share ETA", pendingIntent("SHARE_EN")).build())
            .addAction(Notification.Action.Builder(null, "前一步", pendingIntent("PREV")).build())
            .addAction(Notification.Action.Builder(null, "終止行程", pendingIntent("END")).build())
            .build()
        val update = requireNotNull(CitymapperNotificationExtractor.extract(context, source(notification)))
        val rendered = LiveStatusReminder.buildCitymapperNotification(context, update)
        assertEquals(listOf("前一步", "終止行程"), rendered.actions.map { it.title.toString() })
        assertEquals("示例廣場\n預計抵達時間：下午6:21到達", rendered.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
        assertEquals("步行", rendered.shortCriticalText)
        assertEquals(R.drawable.ic_walking_notification, rendered.smallIcon.resId)
    }

    @Test
    fun observedTaiwanStagesRenderLocalizedCriticalTextAndTransportIcons() {
        val cases = listOf(
            "步行至公車站\n(距離8分鐘)\n示例站" to ("8分步行" to R.drawable.ic_walking_notification),
            "等候 107 或 201 (示例站)\n11, 12, 18分鐘" to ("11分公車" to R.drawable.ic_bus_notification),
            "等候 107 或 201 (示例站)\n下午2:03, 下午2:04" to ("2:03發車" to R.drawable.ic_bus_notification),
            "14:18 1201 新竹-Hsinchu\n下午3:11 (95分鐘)到達" to ("14:18發車" to R.drawable.ic_train_notification),
            "等候 BL (往亞東醫院)\n3, 8, 16分鐘" to ("3分捷運" to R.drawable.ic_metro_notification),
            "等候 R (示例站)\n3, 8, 16分鐘" to ("3分捷運" to R.drawable.ic_metro_notification),
            "Wait for shuttle (Example Stop)\n2:03 PM, 2:04 PM" to ("2:03發車" to R.drawable.ic_transit_notification),
        )
        for ((text, expected) in cases) {
            val notification = Notification.Builder(context, "trip-progress")
                .setSmallIcon(R.drawable.ic_navigation_notification)
                .setContentText(text)
                .setOngoing(true)
                .build()
            val update = requireNotNull(CitymapperNotificationExtractor.extract(context, source(notification)))
            val rendered = LiveStatusReminder.buildCitymapperNotification(context, update)
            assertEquals(expected.first, rendered.shortCriticalText)
            assertEquals(expected.second, rendered.smallIcon.resId)
        }
    }

    @Test
    fun sharedTimingFormatsRenderForEveryKnownAndUnknownTransitMode() {
        val countdown = CitymapperTransitTiming.CountdownMinutes(3)
        val scheduled = CitymapperTransitTiming.ScheduledTime("下午2:03")
        val countdownCases = listOf(
            CitymapperTransitMode.BUS to "3分公車",
            CitymapperTransitMode.TRAIN to "3分台鐵",
            CitymapperTransitMode.METRO to "3分捷運",
            CitymapperTransitMode.UNKNOWN to "3分到站",
        )
        for ((mode, expected) in countdownCases) {
            val presentation = CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING,
                mode,
                transitTiming = countdown,
            )
            assertEquals(expected, LiveStatusReminder.citymapperCriticalText(context, presentation))
        }
        for (mode in CitymapperTransitMode.entries) {
            val presentation = CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING,
                mode,
                transitTiming = scheduled,
            )
            assertEquals("2:03發車", LiveStatusReminder.citymapperCriticalText(context, presentation))
        }
        assertEquals("9:05發車", LiveStatusReminder.citymapperScheduledCriticalText(context, "上午9:05"))
        assertEquals("18:03發車", LiveStatusReminder.citymapperScheduledCriticalText(context, "18:03"))
        assertEquals("2:03發車", LiveStatusReminder.citymapperScheduledCriticalText(context, "2:03 PM"))
    }

    @Test
    fun cardSimulationsRenderExpectedChipIconAndFallbackIntent() {
        val expected = listOf(
            CitymapperSimulation.WALKING to ("8分步行" to R.drawable.ic_walking_notification),
            CitymapperSimulation.BUS_WAITING to ("11分公車" to R.drawable.ic_bus_notification),
            CitymapperSimulation.TRAIN_DEPARTURE to ("2:03發車" to R.drawable.ic_train_notification),
            CitymapperSimulation.METRO_WAITING to ("3分捷運" to R.drawable.ic_metro_notification),
            CitymapperSimulation.METRO_RIDING to ("4站下車" to R.drawable.ic_metro_notification),
        )
        val notificationIds = mutableSetOf<Int>()
        for ((simulation, presentation) in expected) {
            val update = simulation.createUpdate()
            val payload = LiveStatusReminder.citymapperNavigationPayload(
                update,
                LiveStatusReminder.citymapperCriticalText(context, update.presentation),
            )
            val rendered = LiveStatusReminder.buildCitymapperNotification(context, update)
            notificationIds += payload.id
            assertEquals(presentation.first, rendered.shortCriticalText)
            assertEquals(presentation.second, rendered.smallIcon.resId)
            assertEquals(update.text.title, rendered.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertTrue(rendered.actions.isNullOrEmpty())
            assertNotNull(rendered.contentIntent)
            assertTrue(rendered.contentIntent.isActivity)
        }
        assertEquals(1, notificationIds.size)
        LiveStatusReminder.clearCitymapperNavigation(context)
    }

    @Test
    fun replacementDropsStaleActionsAndCreatesFallbackOpenIntent() {
        val first = CitymapperNavigationUpdate(
            "trip", 1, CitymapperNavigationText("Wait for bus", "示例站"),
            sourceActions = listOf(Notification.Action.Builder(null, "Next", pendingIntent("NEXT")).build()),
        )
        val initial = LiveStatusReminder.buildCitymapperNotification(context, first)
        val replacement = LiveStatusReminder.buildCitymapperNotification(
            context, first.copy(text = CitymapperNavigationText("Continue", ""), sourceActions = emptyList()),
        )
        assertEquals(1, initial.actions.size)
        assertTrue(replacement.actions.isNullOrEmpty())
        assertNotNull(replacement.contentIntent)
        assertTrue(replacement.contentIntent.isActivity)
        assertEquals("Continue", replacement.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("", replacement.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
    }

    @Test
    fun customTextViewPreservesLinesAndDeduplicatesExpandedView() {
        val views = RemoteViews("android", android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Walk to Bus Stop\n(9 min away)\n示例站\nShare ETA")
        }
        val notification = Notification.Builder(context, "trip-progress")
            .setSmallIcon(R.drawable.ic_navigation_notification)
            .setOngoing(true)
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .build()
        val update = requireNotNull(CitymapperNotificationExtractor.extract(context, source(notification)))
        assertEquals(CitymapperNavigationText("Walk to Bus Stop", "(9 min away)\n示例站"), update.text)
        assertNull(CitymapperNotificationExtractor.extract(context, source(notification, "unrelated.app")))
    }

    @Test
    fun sourceActionIsNotExecutedUntilUserInvokesIt() {
        val actionName = context.packageName + ".CITYMAPPER_TEST_END"
        val invoked = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { invoked.countDown() }
        }
        context.registerReceiver(receiver, IntentFilter(actionName), Context.RECEIVER_NOT_EXPORTED)
        try {
            val intent = PendingIntent.getBroadcast(context, 1717, Intent(actionName).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE)
            val update = CitymapperNavigationUpdate(
                "trip", 1, CitymapperNavigationText("Ride 7 stops to", "示例站"),
                sourceActions = listOf(Notification.Action.Builder(null, "End Trip", intent).build()),
            )
            val rendered = LiveStatusReminder.buildCitymapperNotification(context, update)
            assertEquals(1L, invoked.count)
            rendered.actions.single().actionIntent.send()
            assertTrue(invoked.await(5, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    private fun pendingIntent(suffix: String): PendingIntent = PendingIntent.getBroadcast(
        context, 1717, Intent(context.packageName + ".CITYMAPPER_TEST_" + suffix).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun source(notification: Notification, packageName: String = CitymapperNavigationMapper.PACKAGE_NAME) = StatusBarNotification(
        packageName, packageName, 42, null, Process.myUid(), 0, 0, notification, Process.myUserHandle(), 100,
    )
}
