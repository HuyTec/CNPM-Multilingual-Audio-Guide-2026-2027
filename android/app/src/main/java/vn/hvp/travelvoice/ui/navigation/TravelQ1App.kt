package vn.hvp.travelvoice.ui.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import vn.hvp.travelvoice.BuildConfig
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.data.remote.*
import vn.hvp.travelvoice.platform.audio.TravelPlayback
import vn.hvp.travelvoice.platform.location.NearbyLocation
import vn.hvp.travelvoice.platform.maps.TravelMap
import vn.hvp.travelvoice.ui.screens.*

/** Map-first shell. Selection, sheets and settings are independent of the service-owned player. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelQ1App(mapContent: (@Composable (List<ServerLocation>, (String) -> Unit, () -> Unit) -> Unit)? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("travelq1-settings", Context.MODE_PRIVATE) }
    var base by rememberSaveable { mutableStateOf(prefs.getString("server", BuildConfig.API_BASE_URL)!!) }
    val repository = remember(base) { SharedTourRepository(context.applicationContext, base) }
    val updates = remember(repository) { PackageUpdates(repository) }
    val appScope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf("map") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showList by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf(prefs.getString("language", "VI")!!) }
    var mode by rememberSaveable { mutableStateOf("FULL") }
    var rate by rememberSaveable { mutableFloatStateOf(prefs.getFloat("rate", 1f)) }
    var revision by remember { mutableIntStateOf(0) }
    var locations by remember { mutableStateOf<List<ServerLocation>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(false) }
    var guide by remember(selected, language, mode, revision) { mutableStateOf<ServerGuide?>(null) }
    var guideFailed by remember(selected, language, mode, revision) { mutableStateOf(false) }
    var locationReason by rememberSaveable { mutableStateOf(false) }
    var locationNotice by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    var pendingNearby by rememberSaveable { mutableStateOf(false) }
    var focusRequest by remember { mutableIntStateOf(0) }
    var placePanelHeight by remember { mutableIntStateOf(0) }
    var nearby by rememberSaveable { mutableStateOf(false) }
    var position by remember { mutableStateOf<Location?>(null) }
    var playbackError by remember { mutableStateOf(false) }
    val requester = remember { NearbyLocation(context.applicationContext) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(requester, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { requester.cancel(); locating = false } }
        lifecycle.addObserver(observer)
        onDispose { requester.cancel(); lifecycle.removeObserver(observer) }
    }
    fun locate(filterNearby: Boolean) {
        locating = true; locationNotice = 0
        requester.request { result -> locating = false; position = result
            if (result != null) { if (filterNearby) nearby = true; focusRequest++ }
            if (result == null) locationNotice = R.string.q1_location_unavailable }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate(pendingNearby) else locationNotice = R.string.q1_location_denied
    }
    fun requestLocation(filterNearby: Boolean) {
        pendingNearby = filterNearby
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate(filterNearby)
        else locationReason = true
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (!allowed) locationNotice = R.string.q1_notification_denied
    }
    fun home() { selected = null; details = false; profileOpen = false; showList = false; showSearch = false; query = ""; nearby = false; tab = "map" }
    fun select(id: String) { selected = id; mode = "FULL"; details = false; showList = false; showSearch = false }
    fun setLanguage(code: String) { language = code; prefs.edit().putString("language", code).apply() }
    fun setRate(value: Float) { rate = value; prefs.edit().putFloat("rate", value).apply(); TravelPlayback.narrator?.speed(value) }
    fun retry() { repository.invalidateCache(); revision++ }
    fun play(content: ServerGuide) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && !prefs.getBoolean("notification-asked", false)) {
            prefs.edit().putBoolean("notification-asked", true).apply(); notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        playbackError = runCatching { TravelPlayback.play(context, content, base, rate) }.isFailure
    }
    LaunchedEffect(repository, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                while (isActive) {
                    val previous = updates.available.map { it.getString("version") }
                    updates.check()
                    if (previous != updates.available.map { it.getString("version") }) retry()
                    delay(30_000)
                }
            }
            launch {
                while (isActive) {
                    try { VisitorApi.heartbeat(context, base) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* No connection never blocks reading. */ }
                    delay(60_000)
                }
            }
            launch {
                while (isActive) {
                    PlaybackOutbox.retryAutomatically(context, base)
                    delay(15_000)
                }
            }
        }
    }
    DisposableEffect(repository, lifecycle) {
        val manager = context.getSystemService(android.net.ConnectivityManager::class.java)
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { appScope.launch {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    launch { PlaybackOutbox.retryAutomatically(context, base) }
                    updates.check(); retry()
                }
            } }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    fun openActiveDetails() {
        TravelPlayback.guide?.let { selected = it.locationId; language = it.languageCode; mode = it.scriptType }
        details = true
    }
    LaunchedEffect(repository, revision, language) {
        failed = false
        // Display the bundled, verified baseline immediately while refreshing online content.
        if (locations == null) runCatching { repository.localLocations(language) }.onSuccess { records ->
            locations = records.also { offline = true }
        }
        try { val result = repository.locations(language); locations = result.first; offline = result.second }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (locations.isNullOrEmpty()) failed = true }
    }
    LaunchedEffect(selected, language, mode, revision) {
        val id = selected ?: return@LaunchedEffect
        try { guide = repository.guide(id, language, mode).first }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { guideFailed = true }
    }
    val visible = remember(locations, query, nearby, position) {
        val found = locations.orEmpty().filter { it.name.contains(query, true) }
        if (nearby && position != null) found.filter { distanceMeters(position!!, it)?.let { d -> d <= 500 } == true }
            .sortedBy { distanceMeters(position!!, it) }.take(5) else found
    }
    val place = locations?.firstOrNull { it.id == selected }
    BackHandler(enabled = tab != "map" || selected != null || showList || showSearch) {
        when { tab == "offline" -> tab = "settings"; tab != "map" -> tab = "map"; selected != null -> selected = null; showList -> showList = false; else -> { showSearch = false; query = "" } }
    }
    Scaffold(topBar = { Column { TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
        IconButton(onClick = { profileOpen = true }) { Icon(Icons.Default.AccountCircle, stringResource(R.string.q1_profile)) }
        IconButton(onClick = ::home) { Icon(Icons.Default.Home, stringResource(R.string.q1_home)) }
    })
        if (updates.pending.isNotEmpty() && tab != "offline") Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.q1_new_packages, updates.pending.size), Modifier.weight(1f))
                TextButton(onClick = { tab = "offline"; details = false; profileOpen = false }) { Text(stringResource(R.string.q1_view_packages)) }
            }
        }
    } }, bottomBar = {
        Column {
            if (TravelPlayback.guide != null) TravelMiniPlayer(onOpen = ::openActiveDetails)
            NavigationBar {
                NavigationBarItem(selected = tab == "map", onClick = { tab = "map" }, icon = { Icon(Icons.Default.Map, null) }, label = { Text(stringResource(R.string.q1_map)) })
                NavigationBarItem(selected = details, onClick = { if (selected != null) details = true else openActiveDetails() }, icon = { Icon(Icons.Default.Info, null) }, label = { Text(stringResource(R.string.q1_details)) })
                NavigationBarItem(selected = tab == "offline", onClick = { tab = "offline" }, icon = { Icon(Icons.Default.Download, null) }, label = { Text(stringResource(R.string.q1_offline)) })
                NavigationBarItem(selected = tab == "settings", onClick = { tab = "settings" }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.q1_settings)) })
            }
        }
    }) { padding ->
        when (tab) {
            "settings" -> TravelSettings(language, ::setLanguage, rate, ::setRate, onOffline = { tab = "offline" }, modifier = Modifier.padding(padding))
            "offline" -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { TextButton(onClick = { tab = "settings" }) { Text(stringResource(R.string.back)) }; Text(stringResource(R.string.q1_offline), style = MaterialTheme.typography.headlineSmall) }
                item { SharedOfflineContent(repository, updates, onChanged = ::retry, onOpenLocation = { id -> home(); select(id); details = true }) }
            }
            else -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val wide = maxWidth > maxHeight
                if (mapContent != null) mapContent(visible, ::select, { showList = true })
                else TravelMap(visible, ::select, { showList = true }, Modifier.fillMaxSize(), position, focusRequest,
                    obscuredBottom = if (wide) 0 else placePanelHeight,
                    obscuredRight = if (wide) with(LocalDensity.current) { (maxWidth / 2).roundToPx() } else 0, selectedId = selected)
                Surface(Modifier.align(if (wide) Alignment.TopStart else Alignment.TopCenter).padding(16.dp).fillMaxWidth(if (wide) 0.45f else 1f)
                    .heightIn(max = maxHeight * if (wide) 0.9f else 0.45f), shape = MaterialTheme.shapes.large, tonalElevation = 3.dp) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.q1_discover), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(if (offline) R.string.q1_offline_source else R.string.q1_online_source), style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, stringResource(R.string.q1_search)) }
                        }
                        if (showSearch) OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(stringResource(R.string.q1_search)) }, placeholder = { Text(stringResource(R.string.q1_search_hint)) })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = nearby, onClick = {
                                if (nearby) nearby = false
                                else requestLocation(true)
                            }, label = { Text(stringResource(if (locating) R.string.q1_locating else R.string.q1_nearby)) }, leadingIcon = { Icon(Icons.Default.MyLocation, null, Modifier.size(18.dp)) })
                            AssistChip(onClick = { showList = true }, label = { Text(stringResource(R.string.q1_list)) }, leadingIcon = { Icon(Icons.Default.List, null, Modifier.size(18.dp)) })
                        }
                        if (locationNotice != 0) {
                            Text(stringResource(locationNotice), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text(stringResource(R.string.q1_system_settings)) }
                        }
                    }
                }
                Surface(Modifier.align(if (wide) Alignment.BottomEnd else Alignment.BottomCenter).fillMaxWidth(if (wide) 0.5f else 1f)
                    .heightIn(max = maxHeight * if (wide) 0.94f else 0.5f).onSizeChanged { placePanelHeight = it.height }, shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
                    when {
                        place != null -> TravelPlaceCard(place, guide, guideFailed, position, onRetry = ::retry, onClose = { selected = null },
                            onRead = { details = true }, onPlay = { guide?.let(::play) }, base = base)
                        else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.q1_places, visible.size), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(when { failed -> R.string.q1_error; locations == null -> R.string.q1_loading; visible.isEmpty() && nearby -> R.string.q1_nearby_empty;
                                visible.isEmpty() -> R.string.q1_empty; else -> R.string.q1_discover_hint }), style = MaterialTheme.typography.bodyMedium)
                            if (failed) TextButton(onClick = ::retry) { Text(stringResource(R.string.q1_retry)) }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(visible, key = { it.id }) { item ->
                                OutlinedButton(onClick = { select(item.id) }) { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            } }
                        }
                    }
                }
                val panelHeight = with(LocalDensity.current) { placePanelHeight.toDp() }
                val myLocationLabel = stringResource(R.string.q1_my_location)
                FloatingActionButton(onClick = { if (!locating) requestLocation(false) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(
                        end = if (wide) maxWidth / 2 + 16.dp else 16.dp,
                        bottom = if (wide) 16.dp else (panelHeight + 12.dp).coerceAtMost((maxHeight - 72.dp).coerceAtLeast(0.dp)))
                        .semantics { contentDescription = myLocationLabel },
                    containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
                    if (locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.MyLocation, null)
                }
            }
        }
    }
    if (locationReason) AlertDialog(onDismissRequest = { locationReason = false }, title = { Text(stringResource(R.string.q1_location_title)) },
        text = { Text(stringResource(R.string.q1_location_reason)) }, confirmButton = { TextButton(onClick = {
            locationReason = false; permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }) { Text(stringResource(R.string.q1_allow)) } }, dismissButton = { TextButton(onClick = { locationReason = false }) { Text(stringResource(R.string.q1_later)) } })
    if (showList) ModalBottomSheet(onDismissRequest = { showList = false }) {
        LazyColumn(Modifier.fillMaxHeight(0.8f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(stringResource(R.string.q1_list), style = MaterialTheme.typography.headlineSmall) }
            if (visible.isEmpty()) item { Text(stringResource(if (nearby) R.string.q1_nearby_empty else R.string.q1_empty)) }
            items(visible, key = { it.id }) { item -> ListItem(headlineContent = { Text(item.name) }, supportingContent = { Text(item.category) },
                trailingContent = { TextButton(onClick = { select(item.id) }) { Text(stringResource(R.string.q1_read_more)) } }) }
        }
    }
    if (details && place != null) ModalBottomSheet(onDismissRequest = { details = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        TravelPlaceDetails(place, guide, guideFailed, language, ::setLanguage, mode, { mode = it }, onRetry = ::retry, onPlay = { guide?.let(::play) }, onRate = ::setRate, base = base)
    }
    if (details && place == null) ModalBottomSheet(onDismissRequest = { details = false }) { TravelPlayerPanel(::setRate) }
    if (profileOpen) ModalBottomSheet(onDismissRequest = { profileOpen = false }) {
        TravelProfile(base, onServer = { next ->
            if (next != base) {
                TravelPlayback.stop(context); prefs.edit().putString("server", next).apply(); base = next
                locations = null; selected = null; details = false; revision++
            }
        }, onPackages = { profileOpen = false; tab = "offline" })
    }
    if (playbackError) AlertDialog(onDismissRequest = { playbackError = false }, text = { Text(stringResource(R.string.q1_playback_error)) },
        confirmButton = { TextButton(onClick = { playbackError = false }) { Text(stringResource(R.string.q1_close)) } })
}

internal fun distanceMeters(origin: Location, place: ServerLocation): Float? {
    val lat = place.latitude ?: return null; val lng = place.longitude ?: return null
    if (!lat.isFinite() || !lng.isFinite()) return null
    return FloatArray(1).also { Location.distanceBetween(origin.latitude, origin.longitude, lat, lng, it) }[0]
}
