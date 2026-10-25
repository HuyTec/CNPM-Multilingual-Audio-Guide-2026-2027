package vn.hvp.travelvoice.data.remote

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Same signed schemaVersion=1 package consumed by Web; one AtomicFile per scope. */
class SharedTourRepository(private val context: Context, val base: String) {
    private val guideCache = object : LinkedHashMap<String,Pair<Long,ServerGuide>>(16,0.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,Pair<Long,ServerGuide>>?) = size > 32
    }
    fun invalidateCache() { synchronized(guideCache) { guideCache.clear() } }
    companion object {
        private val storageLock = Any()
        val languages = listOf("VI", "EN", "FR", "JA", "ZH", "RU")
        const val TOUR_CATEGORY = "10000000-0000-0000-0000-000000000101"
        fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fun verify(record: JSONObject): JSONObject {
            val manifest = record.getJSONObject("manifest")
            val bytes = Base64.decode(record.getString("raw"), Base64.DEFAULT)
            require(bytes.size.toLong() == manifest.getLong("sizeBytes") && sha(bytes) == manifest.getString("sha256")) { "Checksum gói không khớp" }
            val keyBytes = Base64.decode(record.getString("key"), Base64.DEFAULT)
            require(sha(keyBytes) == manifest.getString("keyId")) { "Khóa xác minh không khớp" }
            val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            val verifier = Signature.getInstance("SHA256withRSA"); verifier.initVerify(key); verifier.update(bytes)
            require(verifier.verify(Base64.decode(manifest.getString("signature"), Base64.DEFAULT))) { "Chữ ký gói không hợp lệ" }
            val payload = JSONObject(bytes.toString(Charsets.UTF_8))
            val scope = payload.getString("scope")
            require(payload.getInt("schemaVersion") == 1 && payload.getString("status") == "APPROVED"
                && scope in listOf("BASE", "OPTIONAL") && scope == manifest.getString("scope")
                && payload.getBoolean("isBase") == (scope == "BASE") && payload.getBoolean("isBase") == manifest.getBoolean("isBase")
                && manifest.getString("version") == scope.lowercase() + "-" + manifest.getString("sha256")) { "Cấu trúc gói không hợp lệ" }
            return payload
        }
    }
    private val directory = File(context.filesDir, "shared-content-v1").apply { mkdirs() }
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private fun file(scope: String): AtomicFile {
        require(scope in listOf("BASE", "OPTIONAL")); return AtomicFile(File(directory, "$scope.json"))
    }
    private fun readRecords(): List<Pair<JSONObject, JSONObject>> = synchronized(storageLock) { readRecordsUnlocked() }
    private fun readRecordsUnlocked(): List<Pair<JSONObject, JSONObject>> {
        val records = listOf("BASE", "OPTIONAL").mapNotNull { scope ->
        val record = runCatching {
            JSONObject(file(scope).openRead().bufferedReader().use { it.readText() })
        }.getOrElse {
            if (scope != "BASE") return@mapNotNull null
            runCatching { JSONObject(context.assets.open("content/base.json").bufferedReader().use { it.readText() }) }.getOrNull()
        } ?: return@mapNotNull null
        record to verify(record)
        }
        val bundled = verify(JSONObject(context.assets.open("content/base.json").bufferedReader().use { it.readText() }))
        val retired = (records.map { it.second } + bundled).flatMap { payload ->
            val ids = payload.optJSONArray("retiredLocationIds") ?: JSONArray()
            List(ids.length()) { ids.getString(it) }
        }.toSet()
        return records.map { (record,payload) ->
            // Signature is verified before applying signed revocations; never modify the stored package.
            val visible = JSONObject(payload.toString())
            for (field in listOf("locations","guides")) {
                val items = payload.getJSONArray(field); val kept = JSONArray()
                for (i in 0 until items.length()) if (items.getJSONObject(i).getString("locationId") !in retired) kept.put(items.getJSONObject(i))
                visible.put(field,kept)
            }
            record to visible
        }
    }
    suspend fun installed(): List<JSONObject> = withContext(Dispatchers.IO) { readRecords().map { it.first } }
    suspend fun localLocations(language: String = "VI"): List<ServerLocation> = withContext(Dispatchers.IO) {
        readRecords().flatMap { (_,payload) ->
            val items = payload.getJSONArray("locations")
            List(items.length()) { i -> val item = items.getJSONObject(i)
                ServerLocation(item.getString("locationId"),item.optJSONObject("names")?.optString(language.lowercase())?.takeIf(String::isNotBlank)
                    ?: item.optJSONObject("names")?.optString("vi")?.takeIf(String::isNotBlank) ?: item.getString("name"),item.optDouble("latitude"),item.optDouble("longitude"),item.optString("categoryName"),item.optString("categoryId")) }
        }.distinctBy { it.id }
    }

    suspend fun locations(language: String = "VI"): Pair<List<ServerLocation>, Boolean> {
        try { return DemoApi.locations(base, language) to false }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: GuideApiException) { if (error.status < 500) throw error }
        catch (_: IOException) { /* Fall back only to verified installed/bundled content. */ }
        return withContext(Dispatchers.IO) {
            val locations = localLocations(language)
            require(locations.isNotEmpty()) { "Chưa có gói cơ bản đã xác minh. Cần chuẩn bị nội dung khi có mạng." }
            locations to true
        }
    }
    suspend fun guide(id: String, language: String, mode: String): Pair<ServerGuide, Boolean> {
        val key = "$id/$language/$mode"
        synchronized(guideCache) { guideCache[key]?.let { if (android.os.SystemClock.elapsedRealtime() - it.first < 60_000) return it.second to false } }
        try {
            val content = DemoApi.guide(base, id, language, mode)
            synchronized(guideCache) { guideCache[key] = android.os.SystemClock.elapsedRealtime() to content }
            return content to false
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: GuideApiException) { synchronized(guideCache) { guideCache.remove(key) }; if (error.status < 500) throw error }
        catch (_: IOException) { }
        return withContext(Dispatchers.IO) {
            for (lang in listOf(language, "EN", "VI").distinct()) for ((record, payload) in readRecords()) {
                val guides = payload.getJSONArray("guides")
                for (i in 0 until guides.length()) {
                    val guide = guides.getJSONObject(i)
                    if (guide.getString("locationId") != id || guide.getString("languageCode") != lang || guide.getString("scriptType") != mode) continue
                    var audio: String? = null
                    val assets = record.optJSONArray("downloadedAssets") ?: JSONArray()
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        if (asset.getString("url") != guide.optString("audioUrl")) continue
                        val bytes = Base64.decode(asset.getString("data"), Base64.DEFAULT)
                        val expected = payload.getJSONArray("assets")
                        val entry = (0 until expected.length()).map { expected.getJSONObject(it) }.first { it.getString("url") == asset.getString("url") }
                        require(sha(bytes) == entry.getString("sha256")) { "Audio đã lưu không còn toàn vẹn" }
                        val target = File(context.cacheDir, "${sha(bytes)}.wav"); target.writeBytes(bytes); audio = target.absolutePath
                    }
                    return@withContext ServerGuide(guide.getString("contentId"), guide.getString("locationName"), lang, mode,
                        guide.getString("scriptText"), audio, id, offline = true) to true
                }
            }
            error("Nội dung đang được cập nhật; chưa có bản đã duyệt trong gói đã lưu.")
        }
    }
    private suspend fun get(path: String): ByteArray {
        repeat(3) { attempt ->
            try { return getOnce(path) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: IOException) { if (attempt == 2) throw failure; kotlinx.coroutines.delay(500L * (attempt + 1)) }
        }
        error("Không tải được tài nguyên")
    }
    private suspend fun getOnce(path: String): ByteArray = suspendCancellableCoroutine { continuation ->
        require(path.startsWith("/api/content-packages") || path.matches(Regex("/demo-audio/[a-z-]+\\.wav"))
            || path.matches(Regex("/api/content-audio/[0-9a-f-]{36}\\.(mp3|wav|ogg)")))
        val call = client.newCall(Request.Builder().url(base.trimEnd('/') + path).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) { if (continuation.isActive) continuation.resumeWithException(exception) }
            override fun onResponse(call: Call, response: Response) {
                response.use { result ->
                    try {
                        check(result.isSuccessful) { "Server trả lỗi ${result.code}" }
                        val bytes = result.body?.bytes() ?: error("Gói rỗng")
                        require(bytes.size < 150 * 1024 * 1024) { "Gói quá lớn" }
                        if (continuation.isActive) continuation.resume(bytes)
                    } catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
                }
            }
        })
    }
    suspend fun available(): List<JSONObject> {
        val list = JSONArray(get("/api/content-packages").toString(Charsets.UTF_8))
        return List(list.length()) { list.getJSONObject(it).also { item ->
            require(item.getString("status") == "APPROVED" && item.getString("scope") in listOf("BASE", "OPTIONAL"))
        } }
    }
    suspend fun download(manifest: JSONObject, progress: (String) -> Unit) = withContext(Dispatchers.IO) {
        val scope = manifest.getString("scope"); require(scope in listOf("BASE", "OPTIONAL"))
        val key = JSONObject(get("/api/content-packages/key").toString(Charsets.UTF_8)).getString("spki")
        val url = manifest.getString("downloadUrl")
        require(url == "/api/content-packages/$scope/${manifest.getString("version")}")
        val required = manifest.getLong("sizeBytes") + manifest.getJSONArray("assets").let { entries -> (0 until entries.length()).sumOf { entries.getJSONObject(it).getLong("sizeBytes") } }
        require(directory.usableSpace > required * 2) { "Không đủ dung lượng; phiên bản cũ được giữ" }
        withContext(Dispatchers.Main) { progress(message(vn.hvp.travelvoice.R.string.q1_package_downloading)) }
        val record = JSONObject().put("manifest", manifest).put("raw", Base64.encodeToString(get(url), Base64.NO_WRAP)).put("key", key)
            .put("installedAt", System.currentTimeMillis())
        val payload = verify(record); val assets = payload.getJSONArray("assets"); val downloaded = JSONArray()
        for (i in 0 until assets.length()) {
            withContext(Dispatchers.Main) { progress(message(vn.hvp.travelvoice.R.string.q1_package_audio, i + 1, assets.length())) }
            val asset = assets.getJSONObject(i); val bytes = get(asset.getString("url"))
            require(bytes.size.toLong() == asset.getLong("sizeBytes") && sha(bytes) == asset.getString("sha256")) { "Audio không toàn vẹn" }
            downloaded.put(JSONObject().put("url", asset.getString("url")).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
        record.put("downloadedAssets", downloaded); currentCoroutineContext().ensureActive()
        vn.hvp.travelvoice.platform.audio.PackagePlaybackGate.install(
            waiting = { progress(message(vn.hvp.travelvoice.R.string.q1_package_wait_playback)) }
        ) {
            currentCoroutineContext().ensureActive()
            synchronized(storageLock) {
                val target = file(scope); val stream = target.startWrite()
                try { stream.write(record.toString().toByteArray(Charsets.UTF_8)); target.finishWrite(stream) }
                catch (error: Exception) { target.failWrite(stream); throw error }
            }
            invalidateCache()
        }
    }
    suspend fun deleteOptional() = withContext(Dispatchers.IO) { synchronized(storageLock) { file("OPTIONAL").delete() } }
    private fun message(id: Int, vararg arguments: Any): String {
        val language = context.getSharedPreferences("travelq1-settings",Context.MODE_PRIVATE).getString("app-language","vi")!!
        val configuration = android.content.res.Configuration(context.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(language)) }
        return context.createConfigurationContext(configuration).getString(id,*arguments)
    }
}
