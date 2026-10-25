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

data class ServerLocation(val id: String, val name: String, val latitude: Double? = null,
                          val longitude: Double? = null, val category: String = "", val categoryId: String = "")
data class ServerGuide(val contentId: String, val locationName: String, val languageCode: String,
                       val scriptType: String, val scriptText: String, val audioUrl: String?, val locationId: String = "",
                       val offline: Boolean = false)
class GuideApiException(val status: Int, message: String) : IllegalStateException(message)

object DemoApi {
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    fun normalizeBaseUrl(value: String): String {
        val uri = URI(value.trim())
        require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null
            && (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/")) {
            "Nhập địa chỉ http:// hoặc https:// hợp lệ, không chứa tài khoản hay query."
        }
        return value.trim().trimEnd('/') + "/"
    }

    suspend fun locations(base: String, language: String = "VI"): List<ServerLocation> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ServerLocation>(); var page = 0
        do {
            val data = json(Request.Builder().url(base + "api/locations?size=50&page=$page").build())
            val array = data.getJSONArray("items")
            repeat(array.length()) { i -> array.getJSONObject(i).let { result.add(ServerLocation(it.getString("locationId"),
                it.optJSONObject("names")?.optString(language.lowercase())?.takeIf(String::isNotBlank)
                    ?: it.optJSONObject("names")?.optString("vi")?.takeIf(String::isNotBlank) ?: it.getString("name"),
                it.optDouble("latitude").takeIf(Double::isFinite), it.optDouble("longitude").takeIf(Double::isFinite),
                it.optString("categoryName"), it.optString("categoryId"))) } }
            page++
        } while (page < data.getInt("totalPages"))
        result.distinctBy { it.id }
    }

    suspend fun guide(base: String, location: String, language: String = "VI", type: String = "SHORT"): ServerGuide = withContext(Dispatchers.IO) {
        require(language.matches(Regex("[A-Z]{2,3}(-[A-Z]{2})?")) && type in listOf("FULL", "SHORT"))
        val data = json(Request.Builder().url(base + "api/locations/$location/guide?language=$language&scriptType=$type").build())
        require(data.getString("locationId") == location && data.getString("scriptType") == type
            && data.getString("languageCode") in listOf(language, "EN", "VI") && data.getString("scriptText").isNotBlank()) { "Bài trả về không khớp lựa chọn." }
        val path = data.optString("audioUrl").takeIf { it.isNotBlank() && it != "null" }
        val audio = path?.let {
            val resolved = URI(base).resolve(it)
            require(resolved.scheme in listOf("http", "https") && resolved.host == URI(base).host && resolved.port == URI(base).port) { "Nguồn audio không thuộc server demo." }
            resolved.toString()
        }
        ServerGuide(data.getString("contentId"), data.getString("locationName"), data.getString("languageCode"),
            data.getString("scriptType"), data.getString("scriptText"), audio, data.getString("locationId"))
    }

    suspend fun playback(base: String, eventId: String, contentId: String, identity: JSONObject? = null,
                         occurredAt: String? = null, offline: Boolean = false) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("eventId", eventId).put("contentId", contentId).put("offline", offline)
        occurredAt?.let { payload.put("occurredAt", it) }
        identity?.optString("installation")?.takeIf { runCatching { java.util.UUID.fromString(it) }.isSuccess }
            ?.let { payload.put("installationId", it).put("platform", "ANDROID") }
        val builder = Request.Builder().url(base + "api/playbacks")
        identity?.optString("installation")?.takeIf { it.isNotBlank() }?.let { builder.header("X-Installation-Key",it) }
        json(builder.post(payload.toString().toRequestBody("application/json".toMediaType())).build())
        Unit
    }

    private fun json(request: Request): JSONObject = client.newCall(request).execute().use { response ->
        val data = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val detail = runCatching { JSONObject(data).optString("detail") }.getOrDefault("")
            throw GuideApiException(response.code, detail.ifBlank { "Server trả lỗi ${response.code}. Kiểm tra server rồi thử lại." })
        }
        JSONObject(data)
    }
}
