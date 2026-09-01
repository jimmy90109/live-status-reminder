package com.github.jimmy90109.livestatus

import org.junit.Assert.*
import org.junit.Test

class CitymapperNavigationTest {
    private companion object {
        const val ARRIVAL_FORMAT = "預計抵達時間：%1\$s到達"
    }

    @Test
    fun replacesObservedChineseArrivalAndShareFooterForWalkingAndRiding() {
        for (title in listOf("步行至", "乘坐 4 站")) {
            val text = map("$title\n示例廣場\n下午6:21 (104分鐘)到達\n分享預計抵達時間")!!
            assertEquals(title, text.title)
            assertEquals("示例廣場\n預計抵達時間：下午6:21到達", text.contentText)
        }
        assertEquals("示例站\n預計抵達時間：上午9:05到達", map("步行至\n示例站\n上午9:05（12分鐘）到達\n分享預計抵達時間")?.contentText)
        assertEquals("示例站\n預計抵達時間：18:21到達", map("步行至\n示例站\n18:21到達")?.contentText)
    }

    @Test
    fun onlyReformatsRecognizedFooterAndNeverDropsUnrelatedLastLines() {
        val body = "下午6:21 (104分鐘)到達\n請留意月台資訊"
        assertEquals(body, map("乘坐 4 站\n$body\n分享預計抵達時間")?.contentText)
        for (line in listOf("下午6:99 (104分鐘)到達", "預計抵達時間未定", "等待 104分鐘", "示例站 (104分鐘)", "Arrive 26:21 PM (104 min)")) {
            assertEquals(line, map("步行至\n$line\n分享預計抵達時間")?.contentText)
        }
        assertEquals("請分享預計抵達時間給朋友", map("步行至\n請分享預計抵達時間給朋友")?.contentText)
        assertNull(map("分享預計抵達時間\nShare ETA"))
    }

    @Test
    fun identifiesOnlyKnownShareActionLabels() {
        for (label in listOf("分享預計抵達時間", "分享行程", "Share ETA", " share eta ", "Share trip")) {
            assertTrue(CitymapperNavigationMapper.isShareLabel(label))
        }
        for (label in listOf(null, "終止行程", "前一步", "下一步", "Next", "End Trip")) {
            assertFalse(CitymapperNavigationMapper.isShareLabel(label))
        }
    }

    @Test
    fun mapsObservedWalkingWaitingAndRidingWithoutInterpretingNumbers() {
        val walking = map("Walk to Bus Stop\n(9 min away)\n示例站-Example Stop\nArrive 6:04 PM (126 min)\nShare ETA")!!
        assertEquals("Walk to Bus Stop", walking.title)
        assertEquals("(9 min away)\n示例站-Example Stop\n預計抵達時間：6:04 PM到達", walking.contentText)
        val waiting = map("Wait for 2088 (示例站-EXAMPLE STATION)\n2, 12, 17 min\nArrive 6:09 PM (130 min)\nShare ETA")!!
        assertEquals("Wait for 2088 (示例站-EXAMPLE STATION)", waiting.title)
        assertEquals("2, 12, 17 min\n預計抵達時間：6:09 PM到達", waiting.contentText)
        val riding = map("Ride 7 stops to\n示例站-EXAMPLE STATION\nArrive 6:08 PM (127 min)\nShare ETA")!!
        assertEquals("Ride 7 stops to", riding.title)
        assertEquals("示例站-EXAMPLE STATION\n預計抵達時間：6:08 PM到達", riding.contentText)
    }

    @Test
    fun classifiesObservedTaiwanCommuteStagesAndValues() {
        assertEquals(
            CitymapperNavigationPresentation(CitymapperNavigationStage.WALKING, walkingMinutes = 8),
            presentation("步行至公車站\n(距離8分鐘)\n義九路口-Yi 9th Rd. Intersection\n下午3:11 (96分鐘)到達\n分享預計抵達時間"),
        )
        assertEquals(
            CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING,
                CitymapperTransitMode.BUS,
                transitTiming = countdown(11),
            ),
            presentation("等候 107 或 107 或 201 或 202 (基隆漁會-Keelung Fishmens Association)\n11, 12, 18分鐘\n下午3:11 (96分鐘)到達"),
        )
        assertEquals(
            CitymapperNavigationPresentation(CitymapperNavigationStage.RIDING, stops = 6),
            presentation("乘坐 6 站\n基隆轉運站-Keelung Transit Station\n下午3:11 (95分鐘)到達"),
        )
        assertEquals(
            CitymapperNavigationPresentation(
                CitymapperNavigationStage.TRAIN_DEPARTURE,
                CitymapperTransitMode.TRAIN,
                transitTiming = scheduled("14:18"),
            ),
            presentation("14:18 1201 新竹-Hsinchu\n下午3:11 (95分鐘)到達"),
        )
        assertEquals(
            CitymapperNavigationPresentation(CitymapperNavigationStage.RIDING, stops = 8),
            presentation("乘坐 8 站\n南港-Nangang\n下午3:11 (94分鐘)到達"),
        )
        assertEquals(
            CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING,
                CitymapperTransitMode.METRO,
                transitTiming = countdown(3),
            ),
            presentation("等候 BL (往亞東醫院 Taipei Nangang Exhibition To Far Eastern Hospital)\n3, 8, 16分鐘\n下午3:11 (94分鐘)到達"),
        )
        assertEquals(
            CitymapperNavigationPresentation(CitymapperNavigationStage.RIDING, stops = 4),
            presentation("乘坐 4 站\n市政府-Taipei City Hall\n下午3:11 (93分鐘)到達"),
        )
    }

    @Test
    fun parsesScheduledBusTimeAndKeepsFirstSourceValue() {
        assertEquals(
            CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING,
                CitymapperTransitMode.BUS,
                transitTiming = scheduled("下午2:03"),
            ),
            presentation(
                "等候 107 或 107 或 201 或 202 或 203 或 204 (基隆漁會-Keelung District Fishmens Association)\n" +
                    "下午2:03, 下午2:04\n下午3:30 (90分鐘)到達\n分享預計抵達時間",
            ),
        )
    }

    @Test
    fun parsesSharedCountdownAndScheduledFormatsForUnknownWaitingModes() {
        val cases = listOf(
            "等候 K (示例站)\n3, 8, 16分鐘" to countdown(3),
            "Wait for shuttle (Example Stop)\n2, 12, 17 min" to countdown(2),
            "等候 K (示例站)\n上午9:05, 上午9:10" to scheduled("上午9:05"),
            "等候 K (示例站)\n18:03，18:04" to scheduled("18:03"),
            "Wait for shuttle (Example Stop)\n2:03 PM, 2:04 PM" to scheduled("2:03 PM"),
        )
        for ((text, timing) in cases) {
            assertEquals(
                text,
                CitymapperNavigationPresentation(
                    CitymapperNavigationStage.WAITING,
                    transitTiming = timing,
                ),
                presentation(text),
            )
        }
    }

    @Test
    fun recognizesAllTaipeiMetroLineCodesWithoutClassifyingOtherLetters() {
        for (lineCode in listOf("BR", "R", "G", "O", "BL", "Y", "br", "bl")) {
            assertEquals(
                lineCode,
                CitymapperTransitMode.METRO,
                presentation("等候 $lineCode (示例站)\n3, 8, 16分鐘").transitMode,
            )
        }
        for (unknownCode in listOf("A", "K", "LRT", "TRA")) {
            assertEquals(
                unknownCode,
                CitymapperTransitMode.UNKNOWN,
                presentation("等候 $unknownCode (示例站)\n3, 8, 16分鐘").transitMode,
            )
        }
    }

    @Test
    fun stageParsingIsConservativeAndKeepsMissingValuesNullable() {
        assertEquals(
            CitymapperNavigationPresentation(CitymapperNavigationStage.WALKING),
            presentation("步行至公車站\n距離8分鐘\n示例站"),
        )
        assertEquals(
            CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING, CitymapperTransitMode.BUS,
            ),
            presentation("等候 107 (示例站)\n即將抵達"),
        )
        assertEquals(
            CitymapperNavigationPresentation(
                CitymapperNavigationStage.WAITING,
                CitymapperTransitMode.METRO,
            ),
            presentation("等候 R (示例站)\n即將抵達"),
        )
        for (text in listOf(
            "等候接駁車\n3分鐘",
            "14:99 1201 新竹-Hsinchu",
            "14:18 新竹-Hsinchu",
            "下一站 4 站",
            "示例站 1201\n下午3:11到達",
        )) {
            assertEquals(text, CitymapperNavigationPresentation(), presentation(text))
        }
        for (text in listOf(
            "等候 K (示例站)\n下午2:99, 下午3:04",
            "等候 K (示例站)\n下午3:30 (90分鐘)到達",
            "等候 K (示例站)\n3, soon分鐘",
            "Wait for shuttle (Example Stop)\n13:03 PM, 2:04 PM",
        )) {
            assertEquals(
                text,
                CitymapperNavigationPresentation(CitymapperNavigationStage.WAITING),
                presentation(text),
            )
        }
    }

    @Test
    fun preservesUnknownLanguagesAndTransportInstructions() {
        val unknown = map("繼續沿河岸前進\n下一個路口左轉\n約 18:30 抵達")!!
        assertEquals("繼續沿河岸前進", unknown.title)
        assertEquals("下一個路口左轉\n約 18:30 抵達", unknown.contentText)
        assertEquals("Weiter geradeaus", map("Weiter geradeaus")?.title)
        assertEquals("", map("Weiter geradeaus")?.contentText)
    }

    @Test
    fun normalizesLinesDeduplicatesAndOnlyRemovesExactActionLines() {
        val text = "  Ride   7 stops to \r\n\t示例站 \r\n\nRide 7 stops to\n示例站\nShare ETA\nEnd Trip\nPrev\nNext\nNext street\nshare eta"
        assertEquals(
            CitymapperNavigationText("Ride 7 stops to", "示例站\nNext street"),
            CitymapperNavigationMapper.map(text, listOf("End Trip", "Prev", "Next"), ARRIVAL_FORMAT),
        )
        assertNull(map(null))
        assertNull(map(" \r\n\t"))
        assertNull(CitymapperNavigationMapper.map("Share ETA\nEnd Trip", listOf("End Trip"), ARRIVAL_FORMAT))
    }

    @Test
    fun requiresExactSourceAndChannelOngoingAndNotSummary() {
        assertTrue(eligible())
        assertFalse(eligible(packageName = "other.app"))
        assertFalse(eligible(packageName = null))
        assertFalse(eligible(channelId = "announcements"))
        assertFalse(eligible(channelId = "TRIP-PROGRESS"))
        assertFalse(eligible(channelId = null))
        assertFalse(eligible(ongoing = false))
        assertFalse(eligible(summary = true))
    }

    @Test
    fun updatesSameSourceAndClearsWhenTextOrEligibilityDisappears() {
        val tracker = CitymapperNavigationTracker()
        val walking = update("trip", 1, "Walk to Bus Stop")
        val riding = update("trip", 2, "Ride 7 stops to")
        assertEquals(CitymapperNavigationDecision.Show(walking), tracker.onPosted("trip", walking))
        assertEquals(CitymapperNavigationDecision.Show(riding), tracker.onPosted("trip", riding))
        assertEquals(CitymapperNavigationDecision.None, tracker.onPosted("other", null))
        assertEquals(CitymapperNavigationDecision.Clear, tracker.onPosted("trip", null))
        assertEquals(CitymapperNavigationDecision.None, tracker.onRemoved("trip"))
    }

    @Test
    fun unrelatedAndPreviousSourceRemovalDoesNotClearCurrentTrip() {
        val tracker = CitymapperNavigationTracker()
        tracker.onPosted("old", update("old", 1))
        tracker.onPosted("new", update("new", 2))
        assertEquals(CitymapperNavigationDecision.None, tracker.onRemoved("old"))
        assertEquals(CitymapperNavigationDecision.Clear, tracker.onRemoved("new"))
        assertEquals(CitymapperNavigationDecision.None, tracker.onRemoved("new"))
    }

    @Test
    fun restoreSelectsLatestAndEmptySnapshotClearsStaleReminder() {
        val tracker = CitymapperNavigationTracker()
        val latest = update("latest", 10)
        assertEquals(CitymapperNavigationDecision.Show(latest), tracker.restore(listOf(update("old", 1), latest, update("middle", 5))))
        assertEquals(CitymapperNavigationDecision.None, tracker.onRemoved("old"))
        assertEquals(CitymapperNavigationDecision.Clear, tracker.restore(emptyList()))
        assertEquals(CitymapperNavigationDecision.None, tracker.onRemoved("latest"))
    }

    @Test
    fun disableAndDisconnectResetTrackingWithoutRetainingText() {
        val tracker = CitymapperNavigationTracker()
        tracker.onPosted("trip", update("trip", 1))
        assertEquals(CitymapperNavigationDecision.Clear, tracker.reset())
        assertEquals(CitymapperNavigationDecision.None, tracker.onRemoved("trip"))
        assertTrue(tracker.restore(listOf(update("trip", 2))) is CitymapperNavigationDecision.Show)
    }

    @Test
    fun trackerCarriesObservedModeToRidingAndClearsItAtBoundaries() {
        val tracker = CitymapperNavigationTracker()
        val bus = tracker.onPosted("trip", update("trip", 1, presentation = waiting(CitymapperTransitMode.BUS))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.BUS, bus.update.presentation.transitMode)
        val busRide = tracker.onPosted("trip", update("trip", 2, presentation = riding(6))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.BUS, busRide.update.presentation.transitMode)

        tracker.onPosted("trip", update("trip", 3, presentation = trainDeparture()))
        val trainRide = tracker.onPosted("trip", update("trip", 4, presentation = riding(8))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.TRAIN, trainRide.update.presentation.transitMode)

        tracker.onPosted("trip", update("trip", 5, presentation = waiting(CitymapperTransitMode.METRO)))
        val metroRide = tracker.onPosted("trip", update("trip", 6, presentation = riding(4))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.METRO, metroRide.update.presentation.transitMode)

        tracker.onPosted("trip", update("trip", 7, presentation = CitymapperNavigationPresentation(CitymapperNavigationStage.WALKING)))
        val afterWalking = tracker.onPosted("trip", update("trip", 8, presentation = riding(2))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.UNKNOWN, afterWalking.update.presentation.transitMode)

        tracker.onPosted("trip", update("trip", 9, presentation = waiting(CitymapperTransitMode.BUS)))
        val newSource = tracker.onPosted("new", update("new", 10, presentation = riding(3))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.UNKNOWN, newSource.update.presentation.transitMode)

        tracker.onPosted("trip", update("trip", 11, presentation = waiting(CitymapperTransitMode.BUS)))
        tracker.onPosted("trip", update("trip", 12, presentation = waiting(CitymapperTransitMode.UNKNOWN)))
        val afterUnknownWaiting = tracker.onPosted("trip", update("trip", 13, presentation = riding(2))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.UNKNOWN, afterUnknownWaiting.update.presentation.transitMode)
    }

    @Test
    fun restoreAndResetNeverInventModeForAnIsolatedRidingStage() {
        val tracker = CitymapperNavigationTracker()
        tracker.onPosted("trip", update("trip", 1, presentation = waiting(CitymapperTransitMode.BUS)))
        tracker.reset()
        val afterReset = tracker.onPosted("trip", update("trip", 2, presentation = riding(6))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.UNKNOWN, afterReset.update.presentation.transitMode)

        tracker.onPosted("trip", update("trip", 3, presentation = waiting(CitymapperTransitMode.METRO)))
        val restored = tracker.restore(listOf(update("trip", 4, presentation = riding(4)))) as CitymapperNavigationDecision.Show
        assertEquals(CitymapperTransitMode.UNKNOWN, restored.update.presentation.transitMode)
    }

    @Test
    fun payloadChoosesStageAndTransportIconsWithoutInventingTimerOrProgress() {
        val cases = listOf(
            CitymapperNavigationPresentation(CitymapperNavigationStage.UNKNOWN) to R.drawable.ic_navigation_notification,
            CitymapperNavigationPresentation(CitymapperNavigationStage.WALKING, walkingMinutes = 8) to R.drawable.ic_walking_notification,
            waiting(CitymapperTransitMode.BUS) to R.drawable.ic_bus_notification,
            waiting(CitymapperTransitMode.METRO) to R.drawable.ic_metro_notification,
            trainDeparture() to R.drawable.ic_train_notification,
            waiting(CitymapperTransitMode.UNKNOWN) to R.drawable.ic_transit_notification,
            CitymapperNavigationPresentation(CitymapperNavigationStage.WAITING) to R.drawable.ic_navigation_notification,
            riding(4) to R.drawable.ic_transit_notification,
            riding(4).copy(transitMode = CitymapperTransitMode.BUS) to R.drawable.ic_bus_notification,
            riding(4).copy(transitMode = CitymapperTransitMode.TRAIN) to R.drawable.ic_train_notification,
            riding(4).copy(transitMode = CitymapperTransitMode.METRO) to R.drawable.ic_metro_notification,
        )
        for ((presentation, icon) in cases) {
            val update = update("trip", 1, presentation = presentation)
            val payload = LiveStatusReminder.citymapperNavigationPayload(update, "膠囊")
            assertEquals("膠囊", payload.criticalText)
            assertEquals(update.text.title, payload.title)
            assertEquals(update.text.contentText, payload.contentText)
            assertEquals(icon, payload.smallIconRes)
            assertEquals(icon, payload.leftIconRes)
            assertNull(payload.timer)
            assertNull(payload.progress)
        }
    }

    private fun map(text: String?) = CitymapperNavigationMapper.map(text, arrivalTimeFormat = ARRIVAL_FORMAT)
    private fun presentation(text: String) = requireNotNull(CitymapperNavigationMapper.presentation(text))
    private fun update(
        key: String,
        time: Long,
        title: String = "導航原文",
        presentation: CitymapperNavigationPresentation = CitymapperNavigationPresentation(),
    ) = CitymapperNavigationUpdate(
        key, time, CitymapperNavigationText(title, "示例站\nArrive 6:08 PM (127 min)"), presentation,
    )
    private fun waiting(mode: CitymapperTransitMode) = CitymapperNavigationPresentation(
        CitymapperNavigationStage.WAITING, mode, transitTiming = countdown(3),
    )
    private fun riding(stops: Int) = CitymapperNavigationPresentation(
        CitymapperNavigationStage.RIDING, stops = stops,
    )
    private fun trainDeparture() = CitymapperNavigationPresentation(
        CitymapperNavigationStage.TRAIN_DEPARTURE,
        CitymapperTransitMode.TRAIN,
        transitTiming = scheduled("14:18"),
    )
    private fun countdown(minutes: Int) = CitymapperTransitTiming.CountdownMinutes(minutes)
    private fun scheduled(time: String) = CitymapperTransitTiming.ScheduledTime(time)
    private fun eligible(
        packageName: String? = CitymapperNavigationMapper.PACKAGE_NAME,
        channelId: String? = CitymapperNavigationMapper.CHANNEL_ID,
        ongoing: Boolean = true,
        summary: Boolean = false,
    ) = CitymapperNavigationMapper.isEligible(packageName, channelId, ongoing, summary)
}
