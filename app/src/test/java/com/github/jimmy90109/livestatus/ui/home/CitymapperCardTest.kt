package com.github.jimmy90109.livestatus.ui.home

import com.github.jimmy90109.livestatus.CitymapperNavigationStage
import com.github.jimmy90109.livestatus.CitymapperTransitMode
import com.github.jimmy90109.livestatus.CitymapperTransitTiming
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CitymapperCardTest {
    @Test
    fun coreSimulationsProvideExpectedPresentationTextAndPayload() {
        val cases = listOf(
            ExpectedSimulation(
                CitymapperSimulation.WALKING,
                CitymapperNavigationStage.WALKING,
                CitymapperTransitMode.UNKNOWN,
                walkingMinutes = 8,
                criticalText = "8分步行",
                iconRes = R.drawable.ic_walking_notification,
                title = "步行至公車站",
            ),
            ExpectedSimulation(
                CitymapperSimulation.BUS_WAITING,
                CitymapperNavigationStage.WAITING,
                CitymapperTransitMode.BUS,
                timing = CitymapperTransitTiming.CountdownMinutes(11),
                criticalText = "11分公車",
                iconRes = R.drawable.ic_bus_notification,
                title = "等候 107 或 201（示例站）",
            ),
            ExpectedSimulation(
                CitymapperSimulation.TRAIN_DEPARTURE,
                CitymapperNavigationStage.TRAIN_DEPARTURE,
                CitymapperTransitMode.TRAIN,
                timing = CitymapperTransitTiming.ScheduledTime("下午2:03"),
                criticalText = "2:03發車",
                iconRes = R.drawable.ic_train_notification,
                title = "1201 次往新竹-Hsinchu",
            ),
            ExpectedSimulation(
                CitymapperSimulation.METRO_WAITING,
                CitymapperNavigationStage.WAITING,
                CitymapperTransitMode.METRO,
                timing = CitymapperTransitTiming.CountdownMinutes(3),
                criticalText = "3分捷運",
                iconRes = R.drawable.ic_metro_notification,
                title = "等候 BL（往頂埔）",
            ),
            ExpectedSimulation(
                CitymapperSimulation.METRO_RIDING,
                CitymapperNavigationStage.RIDING,
                CitymapperTransitMode.METRO,
                stops = 4,
                criticalText = "4站下車",
                iconRes = R.drawable.ic_metro_notification,
                title = "乘坐 4 站",
            ),
        )

        assertEquals(CitymapperSimulation.entries.size, cases.size)
        for (expected in cases) {
            val update = expected.simulation.createUpdate()
            val payload = LiveStatusReminder.citymapperNavigationPayload(update, expected.criticalText)
            assertEquals(expected.stage, update.presentation.stage)
            assertEquals(expected.mode, update.presentation.transitMode)
            assertEquals(expected.walkingMinutes, update.presentation.walkingMinutes)
            assertEquals(expected.timing, update.presentation.transitTiming)
            assertEquals(expected.stops, update.presentation.stops)
            assertEquals(expected.title, update.text.title)
            assertTrue(update.text.contentText.isNotBlank())
            assertTrue(update.sourceKey.startsWith("citymapper-simulation-"))
            assertEquals(0L, update.postTime)
            assertTrue(update.sourceActions.isEmpty())
            assertEquals(expected.criticalText, payload.criticalText)
            assertEquals(expected.iconRes, payload.smallIconRes)
            assertEquals(expected.iconRes, payload.leftIconRes)
            assertNull(payload.timer)
            assertNull(payload.progress)
            assertFalse(payload.contentText.isBlank())
        }
    }

    private data class ExpectedSimulation(
        val simulation: CitymapperSimulation,
        val stage: CitymapperNavigationStage,
        val mode: CitymapperTransitMode,
        val walkingMinutes: Int? = null,
        val timing: CitymapperTransitTiming? = null,
        val stops: Int? = null,
        val criticalText: String,
        val iconRes: Int,
        val title: String,
    )
}
