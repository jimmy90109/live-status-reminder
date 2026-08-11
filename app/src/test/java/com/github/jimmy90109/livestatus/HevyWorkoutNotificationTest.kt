package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HevyWorkoutNotificationTest {
    private val nowEpochMillis = 1_700_000_000_000L
    private val startedAtEpochMillis = nowEpochMillis - 180_000L

    @Test
    fun parsesObservedActiveSetPayload() {
        val update = parse(
            "肩推（啞鈴）\n第 4/4 組 - 7.5 kg x 12 次\n休息 0:00\n",
        )

        assertEquals(HevyWorkoutPhase.ACTIVE_SET, update?.phase)
        assertEquals("肩推（啞鈴）", update?.exerciseName)
        assertEquals(4, update?.setNumber)
        assertEquals(4, update?.totalSets)
        assertEquals("7.5 kg × 12 次", update?.setDetail)
        assertEquals("第 4/4 組 - 7.5 kg x 12 次", update?.sourceContentText)
        assertEquals(100, update?.progressPercent)
        assertNull(update?.restRemainingSeconds)
    }

    @Test
    fun parsesObservedRestPayload() {
        val update = parse(
            "俯身飛鳥（啞鈴）\n" +
                "下一個: 第1 組（共 4組） (2.5 kg x 12 次)\n" +
                "休息 0:45\n",
        )

        assertEquals(HevyWorkoutPhase.REST, update?.phase)
        assertEquals("俯身飛鳥（啞鈴）", update?.exerciseName)
        assertEquals(1, update?.setNumber)
        assertEquals(4, update?.totalSets)
        assertEquals("2.5 kg × 12 次", update?.setDetail)
        assertEquals(45, update?.restRemainingSeconds)
        assertEquals(25, update?.progressPercent)
    }

    @Test
    fun parsesMultiMinuteRestAndFullWidthColon() {
        val update = parse(
            "深蹲\n下一個：第 2 組（共 5 組）（100 kg × 5 次）\n休息 2:05",
        )

        assertEquals(125, update?.restRemainingSeconds)
        assertEquals("100 kg × 5 次", update?.setDetail)
    }

    @Test
    fun ignoresUnrelatedMalformedAndInvalidPayloads() {
        assertNull(parse("今天完成了一次訓練"))
        assertNull(parse("深蹲\n第 5/4 組 - 100 kg x 5 次"))
        assertNull(parse("深蹲\n下一個: 第1 組（共 4組） (10 kg x 5 次)\n休息 0:99"))
        assertNull(parse("\n\n"))
    }

    @Test
    fun requiresObservedChannelCategoryAndValidStartTime() {
        assertNull(parse("深蹲\n第 1/4 組 - 100 kg x 5 次", channelId = "other"))
        assertNull(parse("深蹲\n第 1/4 組 - 100 kg x 5 次", category = "status"))
        assertNull(parse("深蹲\n第 1/4 組 - 100 kg x 5 次", startedAt = 0L))
        assertNull(
            parse(
                "深蹲\n第 1/4 組 - 100 kg x 5 次",
                startedAt = nowEpochMillis + 1,
            ),
        )
    }

    @Test
    fun trackerClearsOnlyWhenActiveWorkoutIsRemovedOrStopsMatching() {
        val tracker = HevyWorkoutTracker()
        val update = requireNotNull(parse("深蹲\n第 1/4 組 - 100 kg x 5 次"))

        assertTrue(tracker.onPosted(update.sourceKey, update) is HevyWorkoutDecision.Show)
        assertEquals(HevyWorkoutDecision.None, tracker.onRemoved("hevy|other"))
        assertEquals(HevyWorkoutDecision.Clear, tracker.onPosted(update.sourceKey, null))
        assertEquals(HevyWorkoutDecision.None, tracker.onRemoved(update.sourceKey))
    }

    @Test
    fun trackerRestoresNewestWorkoutAndClearsWhenNoneRemain() {
        val tracker = HevyWorkoutTracker()
        val older = requireNotNull(parse("深蹲\n第 1/4 組 - 100 kg x 5 次"))
        val newer = older.copy(
            sourceKey = "hevy|newer",
            startedAtEpochMillis = older.startedAtEpochMillis + 1_000,
        )

        assertEquals(HevyWorkoutDecision.Show(newer), tracker.restore(listOf(newer, older)))
        assertEquals(HevyWorkoutDecision.None, tracker.onRemoved(older.sourceKey))
        assertEquals(HevyWorkoutDecision.Clear, tracker.onRemoved(newer.sourceKey))
        assertEquals(HevyWorkoutDecision.Clear, tracker.restore(emptyList()))
    }

    private fun parse(
        text: String,
        channelId: String = HevyWorkoutNotificationParser.WORKOUT_CHANNEL_ID,
        category: String = HevyWorkoutNotificationParser.WORKOUT_CATEGORY,
        startedAt: Long = startedAtEpochMillis,
    ): HevyWorkoutUpdate? = HevyWorkoutNotificationParser.parse(
        sourceKey = "hevy|workout",
        channelId = channelId,
        category = category,
        startedAtEpochMillis = startedAt,
        notificationText = text,
        nowEpochMillis = nowEpochMillis,
    )
}
