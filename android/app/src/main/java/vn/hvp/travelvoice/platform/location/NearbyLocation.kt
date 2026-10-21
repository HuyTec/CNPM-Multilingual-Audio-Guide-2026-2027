package vn.hvp.travelvoice.platform.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** A user-triggered, bounded request. No continuous/background location subscription is created. */
class NearbyLocation(private val context: Context) {
    private var signal: CancellationSignal? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sequence = 0
    fun request(result: (Location?) -> Unit) {
        cancel()
        val token = sequence
        val manager = context.getSystemService(LocationManager::class.java)
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { result(null); return }
        val provider = when {
            fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> { result(null); return }
        }
        signal = CancellationSignal()
        fun complete(location: Location?) { if (sequence == token) { cancel(); result(location) } }
        handler.postDelayed({ complete(null) }, 15_000)
        try { LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { complete(it) } }
        catch (_: SecurityException) { complete(null) }
        catch (_: IllegalArgumentException) { complete(null) }
    }
    fun cancel() { sequence++; signal?.cancel(); signal = null; handler.removeCallbacksAndMessages(null) }
}
