package com.example.integradorasensorproximidad.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.ui.player.PlayerScreenContent
import com.example.integradorasensorproximidad.ui.viewmodel.PlayerUiState
import com.example.integradorasensorproximidad.ui.viewmodel.PlaylistsUiState
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onSongSelected: (Song) -> Unit, // Parámetro para notificar la selección de una canción
    viewModel: PlaylistDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.playlist?.name ?: "Detalle de Playlist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(uiState.error!!, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadPlaylistDetails() }) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.songs.isEmpty() -> {
                    Text("Esta playlist aún no tiene canciones.", textAlign = TextAlign.Center)
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.songs) { song ->
                            Column(
                                modifier = Modifier.clickable { onSongSelected(song) } // Hacemos el elemento pulsable
                            ) {
                                Text(
                                    text = "${song.title} - ${song.artist}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 16.dp)
                                )
                                Divider() // Separador visual
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------- PREVIEW -------

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlaylistsScreenContentPreview() {

    // Playlists falsas
    val fakePlaylists = listOf(
        Playlist(id = 1, name = "Favoritas ", songIds = listOf(1, 2, 3)),
        Playlist(id = 2, name = "Para Estudiar ", songIds = listOf(4, 5)),
        Playlist(id = 3, name = "Rock", songIds = listOf(6, 7, 8))
    )

    val fakeUiState = PlaylistsUiState(
        isLoading = false,
        error = null,
        playlists = fakePlaylists,
        showCreateDialog = false,
        playlistToDelete = null
    )

    MaterialTheme {
        PlaylistsScreenContent(
            uiState = fakeUiState,
            onShowCreateDialog = {},
            onConfirmCreate = {},
            onDismissCreate = {},
            onDeletePlaylistClick = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onPlaylistClick = {}
        )
    }
}
