package vn.hvp.travelvoice.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

data class ServerLocation(val id: String, val name: String)
data class ServerGuide(val contentId: String, val locationName: String, val languageCode: String,
                       val scriptType: String, val scriptText: String, val audioUrl: String?)

object DemoApi {
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    fun normalizeBaseUrl(value: String): String {
        val uri = URI(value.trim())
        require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Nhập địa chỉ http:// hoặc https:// hợp lệ, không chứa tài khoản hay query."
        }
        return value.trim().trimEnd('/') + "/"
    }

    suspend fun locations(base: String): List<ServerLocation> = withContext(Dispatchers.IO) {
        val array = json(Request.Builder().url(base + "api/locations?size=50").build()).getJSONArray("items")
        List(array.length()) { i -> array.getJSONObject(i).let { ServerLocation(it.getString("locationId"), it.getString("name")) } }
    }

    suspend fun guide(base: String, location: String): ServerGuide = withContext(Dispatchers.IO) {
        val data = json(Request.Builder().url(base + "api/locations/$location/guide?language=VI&scriptType=SHORT").build())
        val path = data.optString("audioUrl").takeIf { it.isNotBlank() && it != "null" }
        val audio = path?.let {
            val resolved = URI(base).resolve(it)
            require(resolved.scheme in listOf("http", "https") && resolved.host == URI(base).host && resolved.port == URI(base).port) { "Nguồn audio không thuộc server demo." }
            resolved.toString()
        }
        ServerGuide(data.getString("contentId"), data.getString("locationName"), data.getString("languageCode"),
            data.getString("scriptType"), data.getString("scriptText"), audio)
    }

    suspend fun playback(base: String, eventId: String, contentId: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("eventId", eventId).put("contentId", contentId).toString()
        json(Request.Builder().url(base + "api/playbacks").post(payload.toRequestBody("application/json".toMediaType())).build())
        Unit
    }

    private fun json(request: Request): JSONObject = client.newCall(request).execute().use { response ->
        val data = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val detail = runCatching { JSONObject(data).optString("detail") }.getOrDefault("")
            error(detail.ifBlank { "Server trả lỗi ${response.code}. Kiểm tra server rồi thử lại." })
        }
        JSONObject(data)
    }
}
