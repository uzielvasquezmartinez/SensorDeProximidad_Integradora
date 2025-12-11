package com.example.integradorasensorproximidad.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.integradorasensorproximidad.ui.screens.PlayerScreen
import com.example.integradorasensorproximidad.ui.viewmodel.PlayerViewModel
import com.example.integradorasensorproximidad.ui.playlists.PlaylistDetailScreen
import com.example.integradorasensorproximidad.ui.playlists.PlaylistsScreen
import com.example.integradorasensorproximidad.ui.screens.AppScreen
import com.example.integradorasensorproximidad.ui.screens.PLAYLIST_ID_ARG

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val screens = listOf(AppScreen.Player, AppScreen.Playlists)

    // Creamos el ViewModel aquí para que sea compartido por todas las pantallas que lo necesiten.
    val playerViewModel: PlayerViewModel = viewModel()

    Scaffold(
        bottomBar = {
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
            composable(AppScreen.Player.route) {
                // Pasamos el ViewModel compartido a la pantalla del reproductor.
                PlayerScreen(viewModel = playerViewModel)
            }
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
                PlaylistDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongSelected = { song ->
                        // 1. Usamos el ViewModel compartido para reproducir la canción.
                        playerViewModel.playSong(song)
                        // 2. Navegamos de vuelta a la pantalla del reproductor.
                        navController.navigate(AppScreen.Player.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    }
                )
            }
        }
    }
}

