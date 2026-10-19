package vn.hvp.travelvoice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import vn.hvp.travelvoice.domain.model.isValidMapCoordinate
import vn.hvp.travelvoice.ui.mock.DemoData

class MapCoordinateTest {
    @Test fun fixtureCoordinatesPreserveLocationIds() {
        assertEquals(DemoData.locations.map { it.id }, DemoData.mapLocations.map { it.id })
        assertEquals(6, DemoData.mapLocations.map { it.id }.distinct().size)
        assertTrue(DemoData.mapLocations.all {
            isValidMapCoordinate(requireNotNull(it.latitude), requireNotNull(it.longitude))
        })
    }

    @Test fun missingOrInvalidCoordinatesCannotBecomeMapMarkers() {
        assertFalse(isValidMapCoordinate(Double.NaN, 105.85))
        assertFalse(isValidMapCoordinate(21.03, Double.POSITIVE_INFINITY))
        assertFalse(isValidMapCoordinate(91.0, 105.85))
        assertFalse(isValidMapCoordinate(21.03, -181.0))
        assertTrue(isValidMapCoordinate(-90.0, 180.0))
        assertTrue(isValidMapCoordinate(0.0, 0.0))
    }
}
