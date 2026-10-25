package vn.hvp.travelvoice.platform.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only published POI images on the configured content server may be fetched. */
internal fun publishedImageUrl(base: String, path: String): String {
    val origin = URI(base)
    val image = origin.resolve(path)
    fun port(uri: URI) = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
    require(image.scheme in listOf("http", "https") && image.scheme == origin.scheme && image.host == origin.host &&
        port(image) == port(origin) && image.userInfo == null && image.query == null && image.fragment == null &&
        image.rawPath.matches(Regex("/api/content-images/[0-9a-fA-F-]{36}\\.png")))
    return image.toString()
}

internal object PublishedImageLoader {
    private const val MAX_BYTES = 10 * 1024 * 1024
    private val bitmaps = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private var client: OkHttpClient? = null
    private fun client(context: Context): OkHttpClient = synchronized(this) {
        client ?: OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
            .cache(Cache(File(context.applicationContext.cacheDir, "published-poi-images"), 32L * 1024 * 1024))
            .build().also { client = it }
    }

    suspend fun load(context: Context, base: String, path: String): Bitmap {
        val url = publishedImageUrl(base, path)
        bitmaps.get(url)?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            val call = client(context).newCall(Request.Builder().url(url).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use { result -> try {
                        check(result.isSuccessful) { "Không tải được ảnh địa điểm" }
                        val body = result.body ?: error("Ảnh rỗng")
                        require(body.contentLength() <= MAX_BYTES)
                        val bytes = body.byteStream().use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= MAX_BYTES)
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        require(options.outWidth in 1..4096 && options.outHeight in 1..4096 &&
                            options.outWidth.toLong() * options.outHeight <= 12_000_000)
                        options.inJustDecodeBounds = false
                        options.inSampleSize = 1
                        while (options.outWidth / options.inSampleSize > 800 || options.outHeight / options.inSampleSize > 800)
                            options.inSampleSize *= 2
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Ảnh không hợp lệ")
                        bitmaps.put(url, bitmap)
                        if (continuation.isActive) continuation.resume(bitmap)
                    } catch (failure: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(failure)
                    } }
                }
            })
        }
    }
}
