package vn.hvp.travelvoice.ui.screens
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import org.json.JSONObject
import vn.hvp.travelvoice.data.remote.SharedTourRepository
import java.text.NumberFormat
import java.util.Locale

/** Foreground polling only; no background push or forced download. */
@Composable
fun PackageUpdateNotice(repository: SharedTourRepository) {
    val context=LocalContext.current; val lifecycle=LocalLifecycleOwner.current.lifecycle
    val scope=rememberCoroutineScope();var networkRevision by remember { mutableIntStateOf(0) }
    var updates by remember(repository) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) };var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(network:Network){networkRevision++}}
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    LaunchedEffect(repository,lifecycle,networkRevision) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive) {
                try {
                    val available=repository.available();val installed=repository.installed()
                    updates=available.filter { m->installed.none { it.getJSONObject("manifest").getString("version")==m.getString("version") } }
                } catch(cancelled:CancellationException){throw cancelled}
                catch(_:Exception){ /* Keep installed content available and retry next interval. */ }
                delay(30000)
            }
        }
    }
    Column {
        if(updates.isNotEmpty()) Text("Có nội dung mới")
        updates.forEach { m->
            val assets=m.getJSONArray("assets")
            val bytes=m.getLong("sizeBytes")+(0 until assets.length()).sumOf { assets.getJSONObject(it).getLong("sizeBytes") }
            Text("${m.getString("scope")} · ${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(bytes/1024)} KB")
            OutlinedButton(enabled=!busy,onClick={selected=m}) { Text("Tải về") }
        }
        if(message.isNotBlank()) Text(message)
    }
    selected?.let { manifest->AlertDialog(onDismissRequest={selected=null},title={Text("Tải nội dung mới?")},text={Text("Chỉ thay phiên bản cũ sau xác minh và khi lượt nghe kết thúc.")},
        confirmButton={TextButton(onClick={selected=null;busy=true;scope.launch {
            try { repository.download(manifest){message=it};updates=updates.filterNot { it.getString("scope")==manifest.getString("scope") };message="Đã tải và xác minh gói." }
            catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){message="Chưa tải được; phiên bản cũ vẫn dùng được."}
            finally{busy=false}
        }}){Text("Xác nhận tải")}},dismissButton={TextButton(onClick={selected=null}){Text("Hủy")}}) }
}
