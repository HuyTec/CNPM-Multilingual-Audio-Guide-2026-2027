package vn.hvp.travelvoice.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-bound AES-GCM storage. Tokens and queued identity snapshots are excluded from backup. */
class TouristIdentity(context: Context) {
    private companion object { val lock = Any() }
    private val prefs = context.applicationContext.getSharedPreferences("tourist-secure-v1", Context.MODE_PRIVATE)
    private fun storageName(name: String) = if (name == "session") "session-" + SharedTourRepository.sha(vn.hvp.travelvoice.BuildConfig.API_BASE_URL.toByteArray()) else name
    private fun key(): SecretKey = synchronized(lock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("travelq1-tourist", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("travelq1-tourist", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun read(name: String): String? {
        val raw = prefs.getString(storageName(name), null) ?: return null
        val bytes = Base64.decode(raw, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }
    fun write(name: String, value: String?) {
        if (value == null) { check(prefs.edit().remove(storageName(name)).commit()); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key())
        check(prefs.edit().putString(storageName(name),Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()),Base64.NO_WRAP)).commit())
    }
    fun installation(): String = synchronized(lock) { read("installation") ?: Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) },Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING).also { write("installation",it) } }
    fun session(): JSONObject? = synchronized(lock) {
        val current = read("session")?.let(::JSONObject) ?: return@synchronized null
        if (Instant.parse(current.getString("expiresAt")).isAfter(Instant.now())) return@synchronized current
        signedOut(); null
    }
    fun snapshot(): JSONObject {
        val current = session()
        return JSONObject().put("installation",installation()).also { record -> current?.let { record.put("token",it.getString("accessToken")).put("userId",it.getString("userId")) } }
    }
    fun signedIn(session: JSONObject) = synchronized(lock) { write("session",session.toString()) }
    fun signedOut() = synchronized(lock) { write("session",null); write("installation",null) }
}

object TouristApi {
    private val client = OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS).build()
    suspend fun post(base: String,path: String,body: JSONObject,identity: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(base + "api/tourist/" + path).header("X-Installation-Key",identity.getString("installation"))
        identity.optString("token").takeIf { it.isNotBlank() }?.let { builder.header("Authorization","Bearer $it") }
        client.newCall(builder.post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GuideApiException(response.code,"Identity request failed")
            if (raw.isBlank()) JSONObject() else JSONObject(raw)
        }
    }
}
