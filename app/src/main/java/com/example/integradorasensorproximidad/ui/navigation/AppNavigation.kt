package com.example.integradorasensorproximidad.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.integradorasensorproximidad.ui.player.PlayerScreen
import com.example.integradorasensorproximidad.ui.playlists.PlaylistDetailScreen
import com.example.integradorasensorproximidad.ui.playlists.PlaylistsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val screens = listOf(AppScreen.Player, AppScreen.Playlists)

    Scaffold(
        bottomBar = {
            // Solo mostramos la barra de navegación en las pantallas principales
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (screens.any { it.route == currentRoute }) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = null) },
                            label = { Text(screen.title!!) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Player.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreen.Player.route) { PlayerScreen() }
            composable(AppScreen.Playlists.route) {
                PlaylistsScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate(AppScreen.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }
            composable(
                route = AppScreen.PlaylistDetail.route,
                arguments = listOf(navArgument(PLAYLIST_ID_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                // FIX: Ya no necesitamos pasar el ID, el ViewModel lo obtiene automáticamente.
                PlaylistDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
