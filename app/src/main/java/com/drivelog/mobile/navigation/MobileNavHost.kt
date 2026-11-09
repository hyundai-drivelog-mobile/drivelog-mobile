package com.drivelog.mobile.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.drivelog.mobile.core.model.*
import com.drivelog.mobile.feature.MobileModel
import com.drivelog.mobile.feature.auth.LoginGate
import com.drivelog.mobile.feature.auth.ProfileScreen
import com.drivelog.mobile.feature.common.AppBottomBar
import com.drivelog.mobile.feature.common.MainTab
import com.drivelog.mobile.feature.editor.EditorScreen
import com.drivelog.mobile.feature.editor.FinalPreviewScreen
import com.drivelog.mobile.feature.home.HomeScreen
import com.drivelog.mobile.feature.journey.JourneyDetailScreen
import com.drivelog.mobile.feature.journey.JourneyListScreen
import com.drivelog.mobile.feature.link.VehicleLinkScreen
import com.drivelog.mobile.feature.splash.SplashScreen
import com.drivelog.mobile.feature.wash.WashDetailScreen
import com.drivelog.mobile.feature.Cards
import com.drivelog.mobile.feature.journey.cardMapPoints
import kotlinx.coroutines.launch
import org.json.JSONObject

private object Route {
    const val Splash = "splash"
    const val Link = "link"
    const val Home = "home"
    const val List = "journeys"
    const val Profile = "profile"
    const val Detail = "journey/{id}"
    const val Editor = "editor/{id}"
    const val Preview = "preview/{id}"
    const val WashDetail = "wash_detail"
    fun detail(id: String) = "journey/$id"
    fun editor(id: String) = "editor/$id"
    fun preview(id: String) = "preview/$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MobileNavHost(model: MobileModel) {
    val nav = rememberNavController()
    val currentEntry by nav.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var demo by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var premiumUnlocked by remember { mutableStateOf(false) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(CardDraft()) }
    val washState = model.washState
    val records = if (demo) DemoJourneys.records else model.records
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) activeId?.let { model.importPhoto(it, uri) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val record = records.find { it.id == activeId }
        if (uri != null && record != null) {
            model.export(uri, record, draft.title.ifBlank { record.placeName }, draft.toSettings(), draft.blackTemplate)
        }
    }
    fun pick(id: String) {
        activeId = id
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun edit(id: String) {
        activeId = id
        val record = records.find { it.id == id }
        if (record != null) {
            val stored = model.draft(id)
            val loaded = cardDraftFrom(stored, record.placeName, record.title, record.cardJson)
            draft = if (premiumUnlocked) loaded else loaded.copy(filter = CardFilter.ORIGINAL, layout = CardLayout.PHOTO)
            model.loadPhoto(record)
        }
        nav.navigate(Route.editor(id))
    }
    LaunchedEffect(model.connected) {
        if (model.connected && nav.currentDestination?.route == Route.Link) {
            demo = false
            nav.navigate(Route.Home) { popUpTo(Route.Link) { inclusive = true } }
        }
    }
    LaunchedEffect(model.message) {
        if (model.message.isNotBlank()) {
            snackbar.showSnackbar(model.message)
        }
    }
    fun openTopLevel(route: String) {
        if (currentRoute != route) nav.navigate(route) {
            popUpTo(Route.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = if (currentRoute == Route.Splash) com.drivelog.mobile.ui.theme.DriveColors.Black else com.drivelog.mobile.ui.theme.DriveColors.White,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val tab = when (currentRoute) {
                Route.Home -> MainTab.HOME
                Route.List -> MainTab.RECORDS
                Route.Profile -> MainTab.PROFILE
                else -> null
            }
            if (tab != null) AppBottomBar(tab,
                { openTopLevel(Route.Home) }, { openTopLevel(Route.List) }, { openTopLevel(Route.Profile) })
        }) { padding ->
        NavHost(nav, startDestination = Route.Splash, modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable(Route.Splash) {
                SplashScreen {
                    val target = if (model.connected) model.selected?.let(Route::detail) ?: Route.Home else Route.Link
                    nav.navigate(target) { popUpTo(Route.Splash) { inclusive = true } }
                }
            }
            composable(Route.Link) {
                VehicleLinkScreen(model.busy, model.message, model::claim) {
                    demo = true
                    nav.navigate(Route.Home) { popUpTo(Route.Link) { inclusive = true } }
                }
            }
            composable(Route.Home) {
                LaunchedEffect(Unit) { model.refreshWash() }
                HomeScreen(records, demo, model.connected, { model.photo(it) }, model.photoVersion,
                    { openTopLevel(Route.List) }, { nav.navigate(Route.detail(it)) }, ::edit,
                    { showLogin = true }, { nav.navigate(Route.Link) },
                    { scope.launch { snackbar.showSnackbar("새 주행 기록은 기록 화면에서 확인할 수 있어요") } },
                    {
                        if (nav.currentDestination?.route == Route.Home) {
                            nav.navigate(Route.WashDetail) { launchSingleTop = true }
                        }
                    })
            }
            composable(Route.WashDetail) {
                WashDetailScreen(washState, model.washRegion, model::selectWashRegion,
                    onBack = { nav.popBackStack() })
            }
            composable(Route.List) {
                JourneyListScreen(records, { nav.popBackStack() }, { nav.navigate(Route.detail(it)) }, { if (!demo) model.sync() }, { model.photo(it) }, model.photoVersion)
            }
            composable(Route.Profile) {
                ProfileScreen(model.connected, demo, { nav.navigate(Route.Link) }, { showLogin = true })
            }
            composable(Route.Detail, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                model.photoVersion
                val record = records.find { it.id == entry.arguments?.getString("id") }
                if (record != null) JourneyDetailScreen(record, model.photo(record.id).exists(), { nav.popBackStack() },
                    { pick(record.id) }, { edit(record.id) })
            }
            composable(Route.Editor, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val record = records.find { it.id == entry.arguments?.getString("id") }
                if (record != null) EditorScreen(record, draft, { draft = it }, model.photo(record.id), model.photoVersion,
                    { nav.popBackStack() }, { pick(record.id) }, { nav.navigate(Route.preview(record.id)) },
                    onSave = { if (model.photo(record.id).exists() || cardMapPoints(record, false).size >= 2) {
                        activeId = record.id; exporter.launch("DRIVELOG-${record.id}.png")
                    } else pick(record.id) },
                    premiumUnlocked = premiumUnlocked, onMockPurchase = { premiumUnlocked = true })
            }
            composable(Route.Preview, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val record = records.find { it.id == entry.arguments?.getString("id") }
                if (record != null) FinalPreviewScreen(record, draft, model.photo(record.id), model.photoVersion,
                    { nav.popBackStack() }, { activeId = record.id; exporter.launch("DRIVELOG-${record.id}.png") })
            }
        }
    }
    if (showLogin) ModalBottomSheet(onDismissRequest = { showLogin = false }, containerColor = com.drivelog.mobile.ui.theme.DriveColors.White) {
        LoginGate({ showLogin = false }, { provider ->
            showLogin = false
            scope.launch { snackbar.showSnackbar("$provider 로그인은 준비 중이에요") }
        })
    }
}

private fun cardDraftFrom(stored: JSONObject, placeName: String, recordTitle: String?, cardJson: String?): CardDraft {
    val settings = stored.optJSONObject("settings") ?: Cards.settings(cardJson)
    val crop = settings.optJSONObject("crop") ?: JSONObject()
    return CardDraft(
        title = stored.optString("title", recordTitle ?: placeName),
        filter = CardFilter.entries.find { it.tone == settings.optString("tone", "color") } ?: CardFilter.ORIGINAL,
        layout = CardLayout.entries.find { it.name == settings.optString("template", "PHOTO") } ?: CardLayout.PHOTO,
        showRoute = settings.optBoolean("showRoute", true), showDistance = settings.optBoolean("showDistance", true),
        showTime = settings.optBoolean("showTime", true), showDate = settings.optBoolean("showDate", true),
        showPlace = settings.optBoolean("showPlace", true), showEfficiency = settings.optBoolean("showEfficiency", false),
        blackTemplate = stored.optBoolean("blackTemplate", false),
        trimEnds = settings.optBoolean("trimEnds", true), zoom = crop.optDouble("zoom", 1.0).toFloat().coerceIn(1f, 3f),
        panX = crop.optDouble("x", 0.0).toFloat().coerceIn(-1f, 1f),
        panY = crop.optDouble("y", 0.0).toFloat().coerceIn(-1f, 1f)
    )
}
