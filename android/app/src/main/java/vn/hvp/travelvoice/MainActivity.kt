package vn.hvp.travelvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.hvp.travelvoice.ui.navigation.DemoApp
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme
import vn.hvp.travelvoice.platform.audio.TravelPlayback

class MainActivity : ComponentActivity() {
    override fun onStart() { super.onStart(); TravelPlayback.visible(true) }
    override fun onStop() { TravelPlayback.visible(false); super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TravelVoiceTheme { DemoApp() } }
    }
}
