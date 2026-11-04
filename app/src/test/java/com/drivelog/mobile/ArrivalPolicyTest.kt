package com.drivelog.mobile

import com.drivelog.mobile.data.ArrivalPolicy
import org.junit.Assert.*
import org.junit.Test

class ArrivalPolicyTest {
    @Test fun firstSyncOnlyEstablishesBaseline() { assertTrue(ArrivalPolicy.unseen(null, listOf("a" to false)).isEmpty()) }
    @Test fun ignoresKnownAndPhotographedRecords() { assertEquals(listOf("c"), ArrivalPolicy.unseen(setOf("a"), listOf("a" to false, "b" to true, "c" to false, "c" to false))) }
    @Test fun emptyBaselineStillDetectsFirstArrival() { assertEquals(listOf("a"), ArrivalPolicy.unseen(emptySet(), listOf("a" to false))) }
}
