package vn.hvp.travelvoice

import android.os.Bundle
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import vn.hvp.travelvoice.ui.navigation.DemoApp
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme
import vn.hvp.travelvoice.platform.audio.TravelPlayback

class MainActivity : AppCompatActivity() {
    override fun onStart() { super.onStart(); TravelPlayback.visible(true) }
    override fun onStop() { TravelPlayback.visible(false); super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = getSharedPreferences("travelq1-settings", Context.MODE_PRIVATE)
        val systemChoice = if (android.os.Build.VERSION.SDK_INT >= 33) AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it in listOf("vi","en") } else null
        val language = systemChoice ?: preferences.getString("app-language", "vi")!!
        preferences.edit().putString("app-language",language).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("travelq1-settings", Context.MODE_PRIVATE) }
            var appearance by remember { mutableStateOf(prefs.getString("appearance", "system")) }
            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "appearance") appearance = prefs.getString("appearance", "system")
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val dark = when(appearance) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            TravelVoiceTheme(darkTheme = dark) { DemoApp() }
        }
    }
}
