package com.kusokmedi.waymark.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kusokmedi.waymark.AppContainer
import com.kusokmedi.waymark.data.local.ThemeMode
import com.kusokmedi.waymark.ui.screens.CountryDetailScreen
import com.kusokmedi.waymark.ui.screens.ExploreScreen
import com.kusokmedi.waymark.ui.screens.HistoryScreen
import com.kusokmedi.waymark.ui.screens.OnboardingScreen
import com.kusokmedi.waymark.ui.screens.SettingsHomeScreen
import com.kusokmedi.waymark.ui.screens.AppearanceSettingsScreen
import com.kusokmedi.waymark.ui.screens.SyncSettingsScreen
import com.kusokmedi.waymark.ui.screens.SyncDetailsScreen
import com.kusokmedi.waymark.ui.screens.NotificationSettingsScreen
import com.kusokmedi.waymark.ui.screens.DataManagementScreen
import com.kusokmedi.waymark.ui.screens.AboutSettingsScreen
import com.kusokmedi.waymark.ui.screens.WatchlistScreen
import com.kusokmedi.waymark.ui.theme.WaymarkTheme
import com.kusokmedi.waymark.waymarkContainer

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_COUNTRY_ID = "countryId"
    }

    private var deepLinkCountry by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkCountry = intent?.getStringExtra(EXTRA_COUNTRY_ID)
        val container = application.waymarkContainer()
        setContent {
            WaymarkAppRoot(
                container = container,
                deepLinkCountry = deepLinkCountry,
                onDeepLinkHandled = { deepLinkCountry = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkCountry = intent.getStringExtra(EXTRA_COUNTRY_ID)
    }
}

@Composable
fun WaymarkAppRoot(
    container: AppContainer,
    deepLinkCountry: String?,
    onDeepLinkHandled: () -> Unit,
) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModelFactory(container))
    val settings by viewModel.settings.collectAsState()
    val countries by viewModel.countries.collectAsState()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    WaymarkTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
        if (!settings.onboardingCompleted) {
            OnboardingScreen(
                countries = countries,
                onComplete = { ids, notifications, allowMobileData ->
                    viewModel.completeOnboarding(ids, notifications, settings.themeMode, allowMobileData)
                },
                onRequestSync = { allowMobileData -> viewModel.syncNowWithMobileData(allowMobileData) },
            )
        } else {
            MainNavigation(viewModel, deepLinkCountry, onDeepLinkHandled)
        }
    }
}

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    TopDestination("watchlist", "Watchlist", Icons.Outlined.Checklist),
    TopDestination("explore", "Explore", Icons.Outlined.Explore),
    TopDestination("history", "History", Icons.Outlined.History),
    TopDestination("settings", "Settings", Icons.Outlined.Settings),
)

@Composable
private fun MainNavigation(
    viewModel: AppViewModel,
    deepLinkCountry: String?,
    onDeepLinkHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val watchlist by viewModel.watchlist.collectAsState()
    val countries by viewModel.countries.collectAsState()
    val history by viewModel.history.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route == "watchlist" || route == "explore" || route == "history" || route == "settings"

    fun navigateTopLevel(target: String) {
        if (route == target) return
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(deepLinkCountry) {
        val id = deepLinkCountry
        if (!id.isNullOrBlank()) {
            navController.navigate("country/$id") { launchSingleTop = true }
            onDeepLinkHandled()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = { navigateTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "watchlist",
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(220),
                    initialOffset = { it / 6 },
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220),
                    initialOffset = { it / 6 },
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220),
                    targetOffset = { it / 6 },
                )
            },
        ) {
            composable("watchlist") {
                WatchlistScreen(
                    countries = watchlist,
                    syncState = syncState,
                    onOpenCountry = { navController.navigate("country/$it") },
                    onToggleFavorite = { card -> viewModel.setFavorite(card.id, !card.isFavorite) },
                    onRefresh = viewModel::syncNow,
                    onExplore = { navigateTopLevel("explore") },
                    onOpenSyncDetails = { navController.navigate("settings/sync_details") },
                )
            }
            composable("explore") {
                ExploreScreen(
                    countries = countries,
                    onOpenCountry = { navController.navigate("country/$it") },
                    onToggleFavorite = { card -> viewModel.setFavorite(card.id, !card.isFavorite) },
                    onToggleWatchlist = { card -> viewModel.toggleWatchlist(card.id, card.inWatchlist) },
                )
            }
            composable("history") {
                HistoryScreen(
                    history = history,
                    onOpenCountry = { navController.navigate("country/$it") },
                    onClearHistory = viewModel::clearHistory,
                )
            }
            composable("settings") {
                SettingsHomeScreen(
                    onOpen = { sub -> navController.navigate(sub) },
                )
            }
            composable("settings/appearance") {
                AppearanceSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("settings/sync") {
                SyncSettingsScreen(
                    viewModel = viewModel,
                    syncState = syncState,
                    onBack = { navController.popBackStack() },
                    onOpenDetails = { navController.navigate("settings/sync_details") },
                )
            }
            composable("settings/sync_details") {
                SyncDetailsScreen(
                    syncState = syncState,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings/notifications") {
                NotificationSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("settings/data") {
                DataManagementScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("settings/about") {
                AboutSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                "country/{countryId}",
                arguments = listOf(navArgument("countryId") { type = NavType.StringType }),
            ) { entry ->
                CountryDetailScreen(
                    countryId = entry.arguments?.getString("countryId") ?: return@composable,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
