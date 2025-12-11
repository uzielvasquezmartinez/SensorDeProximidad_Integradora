package com.example.integradorasensorproximidad.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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

    val barColor = Color(0xFF212121)
    val accent = Color(0xA150B09C)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (screens.any { it.route == currentRoute }) {
                BoxWithConstraints {
                    val screenWidth = maxWidth

                    // Altura y tamaños responsivos
                    val barHeight = ((screenWidth.value / 6).coerceIn(50f, 70f)).dp
                    val iconSize = ((screenWidth.value / 15).coerceIn(20f, 32f)).dp
                    val fontSize = ((screenWidth.value / 30).coerceIn(10f, 14f)).sp

                    NavigationBar(
                        modifier = Modifier.height(barHeight),
                        containerColor = barColor,
                        contentColor = accent
                    ) {
                        val currentDestination = navBackStackEntry?.destination

                        screens.forEach { screen ->
                            val selected =
                                currentDestination?.hierarchy?.any { it.route == screen.route } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon!!,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xA150B09C) else accent.copy(alpha = 0.55f),
                                        modifier = Modifier.size(iconSize)
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title!!,
                                        color = if (selected) Color(0xA150B09C) else accent.copy(alpha = 0.55f),
                                        fontSize = fontSize
                                    )
                                },
                                selected = selected,
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
        }
    )
 { innerPadding ->
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


@Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    // Creamos un NavController simulado para el preview
    val navController = rememberNavController()

    // Usamos un ViewModel simulado o un Fake para el preview
    val fakePlayerViewModel: PlayerViewModel = viewModel() // si tienes un constructor vacío o fake

    // Llamamos a tu Composable
    AppNavigationPreviewContent(
        navController = navController,
        playerViewModel = fakePlayerViewModel
    )
}

// Separar el contenido de AppNavigation para inyectar NavController y ViewModel en el preview
@Composable
fun AppNavigationPreviewContent(
    navController: NavHostController,
    playerViewModel: PlayerViewModel
) {
    val screens = listOf(AppScreen.Player, AppScreen.Playlists)
    val barColor = Color(0xFF212121)
    val accent = Color(0xA150B09C)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (screens.any { it.route == currentRoute }) {
                BoxWithConstraints {
                    val screenWidth = maxWidth

                    val barHeight = ((screenWidth.value / 6).coerceIn(50f, 70f)).dp
                    val iconSize = ((screenWidth.value / 15).coerceIn(20f, 32f)).dp
                    val fontSize = ((screenWidth.value / 30).coerceIn(10f, 14f)).sp

                    NavigationBar(
                        modifier = Modifier.height(barHeight),
                        containerColor = barColor,
                        contentColor = accent
                    ) {
                        val currentDestination = navBackStackEntry?.destination

                        screens.forEach { screen ->
                            val selected =
                                currentDestination?.hierarchy?.any { it.route == screen.route } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon!!,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xA150B09C) else accent.copy(alpha = 0.55f),
                                        modifier = Modifier.size(iconSize)
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title!!,
                                        color = if (selected) Color(0xA150B09C) else accent.copy(alpha = 0.55f),
                                        fontSize = fontSize
                                    )
                                },
                                selected = selected,
                                onClick = { /* no necesitamos navegación en el preview */ }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Solo mostramos un Box vacío en el preview, no necesitamos NavHost real
        Box(modifier = Modifier.padding(innerPadding))
    }
}

