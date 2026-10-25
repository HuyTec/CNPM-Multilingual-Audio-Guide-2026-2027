package vn.hvp.travelvoice

import android.app.Application
import android.content.Context

/** Host UI tests use signed bundled content and must never heartbeat a real deployment. */
class OfflineTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        getSharedPreferences("travelq1-settings", Context.MODE_PRIVATE).edit()
            .putString("server", "http://127.0.0.1:1/").commit()
    }
}
