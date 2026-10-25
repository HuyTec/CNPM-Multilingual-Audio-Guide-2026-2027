package vn.hvp.travelvoice.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.platform.images.PublishedImageLoader

/** Published images use the same imageUrl as Web; a failed image never blocks the guide. */
@Composable
fun PublishedLocationPhoto(base: String, imageUrl: String?, name: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    var bitmap by remember(base, imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(base, imageUrl) { mutableStateOf(false) }
    var attempt by remember(base, imageUrl) { mutableIntStateOf(0) }
    LaunchedEffect(base, imageUrl, attempt) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        failed = false
        try { bitmap = PublishedImageLoader.load(context, base, imageUrl).asImageBitmap() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        val photo = bitmap
        if (photo != null) Image(photo, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (!imageUrl.isNullOrBlank() && !failed) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Image, contentDescription = null)
            if (!compact) {
                Text(stringResource(when { imageUrl.isNullOrBlank() -> R.string.q1_no_image; failed -> R.string.q1_photo_error; else -> R.string.q1_photo_loading }))
                if (failed) TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.q1_retry)) }
            }
        }
    }
}
