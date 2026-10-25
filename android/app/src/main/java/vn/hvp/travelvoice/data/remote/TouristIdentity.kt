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
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Anonymous installation ID; the existing AES-GCM store preserves queued listening events. */
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
    fun installation(): String = synchronized(lock) {
        val anonymous = contextPreferences()
        anonymous.getString("id", null) ?: java.util.UUID.randomUUID().toString().also {
            check(anonymous.edit().putString("id", it).commit())
        }
    }
    private val anonymousPrefs = context.applicationContext.getSharedPreferences("anonymous-visitor-v1", Context.MODE_PRIVATE)
    private fun contextPreferences() = anonymousPrefs
    fun snapshot(): JSONObject {
        return JSONObject().put("installation",installation())
    }
}

object VisitorApi {
    private val client = OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).callTimeout(15,TimeUnit.SECONDS).build()
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(android.net.ConnectivityManager::class.java)
        return manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    suspend fun heartbeat(context: Context, base: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("installationId", TouristIdentity(context).installation()).put("platform", "ANDROID")
        val request = Request.Builder().url(base + "api/visitors/heartbeat")
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { check(it.isSuccessful) { "Chưa đồng bộ trạng thái khách" } }
    }
}
