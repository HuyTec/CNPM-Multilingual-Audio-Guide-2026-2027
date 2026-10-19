package vn.hvp.travelvoice.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.ui.components.LocationMapContent
import vn.hvp.travelvoice.ui.components.MapPreview
import vn.hvp.travelvoice.ui.screens.*
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoApp(mapContent: LocationMapContent = { locations, onMarker, onShowList -> MapPreview(locations, onMarker, onShowList = onShowList) }, startDestination: String = "live") {
    val nav = rememberNavController()
    var appLanguage by rememberSaveable { mutableStateOf("VI") }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "explore"
    val root = route == "explore" || route == "offline" || route == "live"
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) }, navigationIcon = {
                if (!root) IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            })
        },
        bottomBar = {
            if (root) NavigationBar {
                listOf("live" to R.string.live_demo, "explore" to R.string.explore, "offline" to R.string.offline).forEach { (destination, label) ->
                    NavigationBarItem(selected = route == destination,
                        onClick = { nav.navigate(destination) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(if (destination == "explore") Icons.Default.Explore else Icons.Default.Download, null) },
                        label = { Text(stringResource(label)) })
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(if (route == "live") "Demo PostgreSQL + audio + thống kê" else "Giao diện mẫu · dữ liệu và audio mô phỏng", Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NavHost(nav, startDestination = startDestination, modifier = Modifier.weight(1f)) {
                composable("live") { LiveDemoScreen(Modifier.fillMaxSize()) }
                composable("explore") { ExploreScreen(onLocation = { nav.navigate("detail/$it") { launchSingleTop = true } }, language = appLanguage, mapContent = mapContent) }
                composable("offline") { OfflineScreen() }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                    DetailScreen(locationId = backStack.arguments?.getString("id").orEmpty(),
                        onPlay = { id, language, mode -> nav.navigate("player/$id/$language/$mode") { launchSingleTop = true } },
                        onBack = { nav.popBackStack() }, initialLanguage = appLanguage, onLanguageChange = { appLanguage = it })
                }
                composable("player/{id}/{language}/{mode}", arguments = listOf("id", "language", "mode").map { navArgument(it) { type = NavType.StringType } }) { backStack ->
                    PlayerScreen(locationId = backStack.arguments?.getString("id").orEmpty(), language = backStack.arguments?.getString("language") ?: "VI", mode = backStack.arguments?.getString("mode") ?: "FULL", onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DemoAppPreview() { TravelVoiceTheme { DemoApp() } }
