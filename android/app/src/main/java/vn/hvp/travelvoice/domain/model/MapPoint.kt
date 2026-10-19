package vn.hvp.travelvoice.domain.model

data class MapPoint(val id: String, val name: String, val latitude: Double, val longitude: Double)

fun isValidMapCoordinate(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)
