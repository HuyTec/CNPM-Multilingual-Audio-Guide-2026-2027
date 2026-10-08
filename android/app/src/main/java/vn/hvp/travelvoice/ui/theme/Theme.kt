package vn.hvp.travelvoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2D6653), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEDE3), onPrimaryContainer = Color(0xFF163D30),
    secondary = Color(0xFF596457), secondaryContainer = Color(0xFFE5EADD),
    background = Color(0xFFF8F9F3), surface = Color(0xFFF8F9F3),
    surfaceContainer = Color(0xFFEEF0E8), surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFEBEEE5), surfaceContainerHighest = Color(0xFFE4E9DF),
    surfaceContainerLowest = Color.White,
    onSurface = Color(0xFF263D43), onSurfaceVariant = Color(0xFF56635F),
    outline = Color(0xFF707B72), outlineVariant = Color(0xFFCDD5CC),
    error = Color(0xFF9D352C), errorContainer = Color(0xFFFFDAD4),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D6BF), onPrimary = Color(0xFF123B2C),
    primaryContainer = Color(0xFF2D5141), onPrimaryContainer = Color(0xFFDBF1E3),
    background = Color(0xFF15201C), surface = Color(0xFF15201C),
    surfaceContainerLow = Color(0xFF202B25), onSurface = Color(0xFFE1EAE4),
    surfaceContainer = Color(0xFF25312A), surfaceContainerHigh = Color(0xFF2D3931),
    surfaceContainerHighest = Color(0xFF354138), surfaceContainerLowest = Color(0xFF101915),
    onSurfaceVariant = Color(0xFFBDC9BF),
)

@Composable
fun TravelVoiceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(20.dp)),
        content = content,
    )
}
