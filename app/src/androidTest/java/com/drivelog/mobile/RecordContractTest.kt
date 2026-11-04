package com.drivelog.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drivelog.mobile.data.RecordDto
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Contract fixtures only; these are not vehicle/server integration results. */
@RunWith(AndroidJUnit4::class)
class RecordContractTest {
    @Test fun nullableDistanceAndObservedCoordinatesSurviveParsing() {
        val record = RecordDto.parse(JSONObject("""{"id":"record-1","placeName":"부산","startedAtMillis":1000,"endedAtMillis":61000,"elapsedSeconds":60,"distanceKm":null,"distanceSource":"NONE","status":"PARTIAL","simulated":true,"route":[[35.1,129.1,1000]],"title":null,"cardJson":null,"hasPhoto":false}"""))
        assertNull(record.distanceKm)
        assertEquals(35.1, record.route.single().latitude, 0.00001)
        assertEquals(129.1, record.route.single().longitude, 0.00001)
        assertEquals("PARTIAL", record.status)
        assertTrue(record.simulated)
    }
}
