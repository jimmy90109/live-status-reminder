package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouBikeAlarmPolicyTest {
    @Test
    fun choosesExactOnlyWhenSpecialAccessIsAvailable() {
        assertEquals(YouBikeAlarmMode.EXACT, YouBikeAlarmPolicy.mode(true))
        assertEquals(YouBikeAlarmMode.INEXACT, YouBikeAlarmPolicy.mode(false))
    }

    @Test
    fun schedulesExactWithoutAlsoSchedulingFallback() {
        var exactScheduled = false
        var inexactScheduled = false

        val mode = YouBikeAlarmPolicy.schedule(
            canScheduleExactAlarms = true,
            scheduleExact = { exactScheduled = true },
            scheduleInexact = { inexactScheduled = true },
        )

        assertEquals(YouBikeAlarmMode.EXACT, mode)
        assertTrue(exactScheduled)
        assertFalse(inexactScheduled)
    }

    @Test
    fun fallsBackWhenPermissionIsMissingOrRevokedDuringScheduling() {
        var inexactCount = 0
        assertEquals(
            YouBikeAlarmMode.INEXACT,
            YouBikeAlarmPolicy.schedule(
                canScheduleExactAlarms = false,
                scheduleExact = { error("Exact scheduling must not run") },
                scheduleInexact = { inexactCount++ },
            ),
        )
        assertEquals(
            YouBikeAlarmMode.INEXACT,
            YouBikeAlarmPolicy.schedule(
                canScheduleExactAlarms = true,
                scheduleExact = { throw SecurityException("revoked") },
                scheduleInexact = { inexactCount++ },
            ),
        )
        assertEquals(2, inexactCount)
    }

    @Test
    fun schedulesJustAfterTheBorrowAnchoredBoundaryWithoutDrift() {
        assertEquals(1_800_001L, YouBikeAlarmPolicy.triggerAtMillis(1_800_000L, 1_000_000L))
        assertEquals(2_001_000L, YouBikeAlarmPolicy.triggerAtMillis(1_800_000L, 2_000_000L))
    }
}
