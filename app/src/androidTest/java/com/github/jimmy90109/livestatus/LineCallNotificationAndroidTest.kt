package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LineCallNotificationAndroidTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun mirrorsSourceIntentsPrivacyAndSourceCallTimer() {
        val hangUp = PendingIntent.getBroadcast(
            context, 19001, Intent("test.line.source.HANG_UP").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            context, 19002, requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        for (visibility in listOf(Notification.VISIBILITY_PUBLIC, Notification.VISIBILITY_PRIVATE, Notification.VISIBILITY_SECRET)) {
            val notification = Notification.Builder(context, LineCallNotificationParser.ONGOING_CHANNEL)
                .setSmallIcon(R.drawable.ic_voice_notification)
                .setStyle(Notification.CallStyle.forOngoingCall(Person.Builder().setName("測試聯絡人").build(), hangUp))
                .setContentText("群組通話 (測試聯絡人)")
                .setContentIntent(open)
                .setCategory(Notification.CATEGORY_CALL)
                .setOngoing(true)
                .setVisibility(visibility)
                .setWhen(123456)
                .build().apply {
                    flags = flags or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_NO_CLEAR
                }
            val source = StatusBarNotification(
                LineCallNotificationParser.PACKAGE_NAME, LineCallNotificationParser.PACKAGE_NAME,
                110000, null, Process.myUid(), 0, 0, notification, Process.myUserHandle(), 1000,
            )
            val update = requireNotNull(LineCallNotificationExtractor.extract(context, source))
            assertEquals(open, update.contentIntent)
            assertEquals(hangUp, update.hangUpAction?.actionIntent)
            val rendered = LiveStatusReminder.buildLineCallNotification(context, update)
            assertEquals(visibility, rendered.visibility)
            assertEquals(open, rendered.contentIntent)
            assertEquals(hangUp, rendered.actions.single().actionIntent)
            assertEquals("android.app.Notification\$BigTextStyle", rendered.extras.getString(Notification.EXTRA_TEMPLATE))
            assertEquals(123456L, update.sourceWhenEpochMillis)
            assertTrue(rendered.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertTrue(rendered.extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
            assertFalse(rendered.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
            assertEquals(123456L, rendered.`when`)
            assertNull(rendered.extras.getCharSequence("android.shortCriticalText"))
            assertTrue(rendered.extras.getBoolean("android.requestPromotedOngoing"))
            assertTrue(rendered.hasPromotableCharacteristics())
            if (!LineCallPolicy.supportsDevice(Build.MANUFACTURER, Build.BRAND)) {
                assertFalse(rendered.extras.containsKey(XiaomiHyperIslandRenderer.FOCUS_PARAM_EXTRA))
            }
            notification.actions = null
            notification.extras.remove("android.hangUpIntent")
            notification.contentIntent = null
            val withoutActions = requireNotNull(LineCallNotificationExtractor.extract(context, source))
            assertNull(withoutActions.hangUpAction)
            assertNull(withoutActions.contentIntent)
            assertTrue(LiveStatusReminder.buildLineCallNotification(context, withoutActions).actions.orEmpty().isEmpty())
            notification.flags = notification.flags or Notification.FLAG_PROMOTED_ONGOING
            assertNull(LineCallNotificationExtractor.extract(context, source))
        }
    }

    @Test
    fun xiaomiIslandUsesNativeCountUpWithSourceTimestamp() {
        val startedAt = 123456L
        val builder = Notification.Builder(context, "test.line.island")
            .setSmallIcon(R.drawable.ic_call_notification)
        XiaomiHyperIslandRenderer.apply(
            context, builder,
            LiveStatusPayload(
                id = LiveStatusReminder.LINE_CALL_NOTIFICATION_ID,
                appName = "LINE",
                smallIconRes = R.drawable.ic_call_notification,
                leftIconRes = R.drawable.ic_call_notification,
                criticalText = "LINE 通話",
                title = "測試",
                contentText = "群組通話",
                countUpStartedAtEpochMillis = startedAt,
            ),
            manufacturer = "Xiaomi", brand = "Redmi",
        )
        val json = JSONObject(builder.build().extras.getString(XiaomiHyperIslandRenderer.FOCUS_PARAM_EXTRA)!!)
        val timer = json.getJSONObject("param_v2").getJSONObject("param_island").getJSONObject("bigIslandArea")
            .getJSONObject("sameWidthDigitInfo").getJSONObject("timerInfo")
        assertEquals(1, timer.getInt("timerType"))
        assertEquals(startedAt, timer.getLong("timerWhen"))
    }

    @Test
    fun invalidSourceTimeDoesNotInventACallTimer() {
        for (whenMillis in listOf(0L, -1L, Long.MAX_VALUE)) {
            val update = LineCallUpdate("source", "測試", "群組通話", 1000, sourceWhenEpochMillis = whenMillis)
            val rendered = LiveStatusReminder.buildLineCallNotification(context, update)
            assertEquals(0L, rendered.`when`)
            assertFalse(rendered.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertFalse(rendered.extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
            assertNull(rendered.extras.getCharSequence("android.shortCriticalText"))
        }
    }

    @Test
    fun debugCanPublishOnPixelAndTurningOffClearsNotification() {
        val preferences = context.getSharedPreferences("app_reminder_preferences", Context.MODE_PRIVATE)
        val key = AppReminderPreferences.App.LINE_CALL.preferenceKey
        val hadValue = preferences.contains(key)
        val previous = preferences.getBoolean(key, false)
        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName, android.Manifest.permission.POST_NOTIFICATIONS,
            )
            preferences.edit().remove(key).commit()
            assertTrue(AppReminderPreferences.App.LINE_CALL.isEnabled(context))
            AppReminderPreferences.App.LINE_CALL.setEnabled(context, true)
            LiveStatusReminder.showLineCall(context, LineCallUpdate("source", "測試", "群組通話", 1000))
            val manager = context.getSystemService(NotificationManager::class.java)
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (manager.activeNotifications.count {
                    it.id == LiveStatusReminder.LINE_CALL_NOTIFICATION_ID
                } != 1 && SystemClock.elapsedRealtime() < deadline
            ) {
                SystemClock.sleep(100)
            }
            assertEquals(1, manager.activeNotifications.count {
                it.id == LiveStatusReminder.LINE_CALL_NOTIFICATION_ID
            })
            AppReminderPreferences.App.LINE_CALL.setEnabled(context, false)
            assertFalse(AppReminderPreferences.App.LINE_CALL.isEnabled(context))
            LiveStatusReminder.showLineCall(context, LineCallUpdate("source", "測試", "群組通話", 1000))
            val clearedDeadline = SystemClock.elapsedRealtime() + 10_000
            while (manager.activeNotifications.any {
                    it.id == LiveStatusReminder.LINE_CALL_NOTIFICATION_ID
                } && SystemClock.elapsedRealtime() < clearedDeadline
            ) {
                SystemClock.sleep(100)
            }
            assertFalse(manager.activeNotifications.any {
                it.id == LiveStatusReminder.LINE_CALL_NOTIFICATION_ID
            })
        } finally {
            if (hadValue) preferences.edit().putBoolean(key, previous).apply()
            else preferences.edit().remove(key).apply()
            LiveStatusReminder.clearLineCall(context)
        }
    }
}
