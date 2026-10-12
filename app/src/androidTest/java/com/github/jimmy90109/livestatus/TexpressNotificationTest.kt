package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TexpressNotificationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersPromotableTextNotificationWithSourceTicketAction() {
        val actionName = context.packageName + ".TEXPRESS_TEST_OPEN"
        val invoked = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                invoked.countDown()
            }
        }
        context.registerReceiver(receiver, IntentFilter(actionName), Context.RECEIVER_NOT_EXPORTED)
        try {
            val openTicket = PendingIntent.getBroadcast(
                context,
                2121,
                Intent(actionName).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val rendered = LiveStatusReminder.buildTexpressArrivalNotification(
                context,
                update(contentIntent = openTicket),
            )

            assertEquals(openTicket, rendered.contentIntent)
            assertEquals("android.app.Notification\$BigTextStyle", rendered.extras.getString(Notification.EXTRA_TEMPLATE))
            assertEquals("5 分鐘後抵達板橋站", rendered.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
            assertFalse(rendered.extras.containsKey(Notification.EXTRA_PICTURE))
            assertFalse(rendered.extras.containsKey(Notification.EXTRA_PICTURE_ICON))
            assertEquals(1, rendered.actions.size)
            assertEquals("開啟乘車碼", rendered.actions.single().title.toString())
            assertEquals(openTicket, rendered.actions.single().actionIntent)
            assertTrue(rendered.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertTrue(rendered.extras.getBoolean("android.requestPromotedOngoing"))
            assertTrue(rendered.hasPromotableCharacteristics())
            assertTrue(rendered.publicVersion.actions.isNullOrEmpty())
            assertEquals(1L, invoked.count)
            rendered.actions.single().actionIntent.send()
            assertTrue(invoked.await(5, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    fun fallbackOpensTexpressFromNotificationAndAction() {
        val rendered = LiveStatusReminder.buildTexpressArrivalNotification(context, update())

        assertNotNull(rendered.contentIntent)
        assertTrue(rendered.contentIntent.isActivity)
        assertEquals(rendered.contentIntent, rendered.actions.single().actionIntent)
    }

    private fun update(contentIntent: PendingIntent? = null) = TexpressArrivalUpdate(
        sourceKey = "texpress",
        postTime = 1L,
        arrival = TexpressArrival("242", 5, "板橋站"),
        contentIntent = contentIntent,
    )
}
