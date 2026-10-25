package vn.hvp.travelvoice.domain.model

data class MapPoint(val id: String, val name: String, val latitude: Double, val longitude: Double)

fun isValidMapCoordinate(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)

/** Keep distant sample POIs as markers without zooming the initial city view out to the world. */
internal fun initialMapViewport(points: List<MapPoint>, anchor: MapCamera): List<MapPoint> {
    val nearby = points.filter { kotlin.math.abs(it.latitude - anchor.latitude) <= 0.5 &&
        kotlin.math.abs(it.longitude - anchor.longitude) <= 0.5 }
    return nearby.ifEmpty { points }
}
