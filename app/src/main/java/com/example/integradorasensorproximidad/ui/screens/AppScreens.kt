package com.example.integradorasensorproximidad.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector

// Constante para el nombre del argumento, para evitar errores de escritura
const val PLAYLIST_ID_ARG = "playlistId"

/**
 * Define las pantallas de la aplicación para la navegación.
 * Title e Icon son opcionales para soportar pantallas de detalle que no están en la barra inferior.
 */
sealed class AppScreen(
    val route: String,
    val title: String? = null,
    val icon: ImageVector? = null
) {
    object Player : AppScreen(
        route = "player",
        title = "Reproductor",
        icon = Icons.Default.MusicNote
    )
    object Playlists : AppScreen(
        route = "playlists",
        title = "Playlists",
        icon = Icons.Default.LibraryMusic
    )

    // Pantalla de detalle para una playlist específica. No tiene título ni ícono para la barra de navegación.
    object PlaylistDetail : AppScreen(
        route = "playlist_detail/{$PLAYLIST_ID_ARG}"
    ) {
        // Función auxiliar para construir la ruta completa con un ID de forma segura
        fun createRoute(playlistId: Int) = "playlist_detail/$playlistId"
    }
}
