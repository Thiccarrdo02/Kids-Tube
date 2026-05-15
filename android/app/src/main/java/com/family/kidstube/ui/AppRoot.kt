package com.family.kidstube.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.family.kidstube.R
import com.family.kidstube.ui.screens.AddVideoScreen
import com.family.kidstube.ui.screens.HomeScreen
import com.family.kidstube.ui.screens.LibraryScreen
import com.family.kidstube.ui.screens.ParentalScreen
import com.family.kidstube.ui.screens.PlayerScreen
import com.family.kidstube.ui.screens.ShortsScreen
import com.family.kidstube.ui.screens.SubscriptionsScreen
import com.family.kidstube.ui.theme.KidsTubeTheme

private sealed class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Home : Tab("home", R.string.tab_home, Icons.Outlined.Home)
    data object Shorts : Tab("shorts", R.string.tab_shorts, Icons.Outlined.PlayCircle)
    data object Subs : Tab("subs", R.string.tab_subscriptions, Icons.Outlined.Subscriptions)
    data object Library : Tab("library", R.string.tab_library, Icons.Outlined.VideoLibrary)
}
private val tabs = listOf(Tab.Home, Tab.Shorts, Tab.Subs, Tab.Library)

@Composable
fun AppRoot() {
    KidsTubeTheme {
        // Activity-scoped VM so all tabs and the player share one feed
        // fetch + one cache + one history snapshot.
        val activity = LocalContext.current as ComponentActivity
        val feedVm: FeedViewModel = viewModel(viewModelStoreOwner = activity)

        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showBar = currentRoute in tabs.map { it.route }

        Scaffold(
            bottomBar = {
                if (showBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            }
        ) { inner ->
            Box(Modifier.fillMaxSize().padding(inner)) {
                NavHost(navController = nav, startDestination = Tab.Home.route) {
                    composable(Tab.Home.route) {
                        HomeScreen(
                            vm = feedVm,
                            onOpenVideo = { id -> nav.navigate("player/$id") },
                            onLogoLongPress5 = { nav.navigate("parental") },
                        )
                    }
                    composable(Tab.Shorts.route) {
                        ShortsScreen(vm = feedVm, onOpenVideo = { id -> nav.navigate("player/$id") })
                    }
                    composable(Tab.Subs.route) {
                        SubscriptionsScreen(vm = feedVm, onOpenVideo = { id -> nav.navigate("player/$id") })
                    }
                    composable(Tab.Library.route) {
                        LibraryScreen(vm = feedVm, onOpenVideo = { id -> nav.navigate("player/$id") })
                    }
                    composable("player/{id}") { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        PlayerScreen(
                            videoId = id, vm = feedVm,
                            onBack = { nav.popBackStack() },
                            onOpenVideo = { next -> nav.navigate("player/$next") },
                        )
                    }
                    composable("parental") {
                        ParentalScreen(
                            vm = feedVm,
                            onBack = { nav.popBackStack() },
                            onOpenAddVideo = { nav.navigate("addvideo") },
                        )
                    }
                    composable("addvideo") {
                        AddVideoScreen(vm = feedVm, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
