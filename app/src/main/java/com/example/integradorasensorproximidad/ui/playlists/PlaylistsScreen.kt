package com.example.integradorasensorproximidad.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.ui.viewmodel.PlaylistsViewModel
import com.example.integradorasensorproximidad.ui.viewmodel.PlaylistsUiState

// =============================================================
// SCREEN PRINCIPAL
// =============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel(),
    onPlaylistClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialogo crear playlist
    if (uiState.showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name -> viewModel.createPlaylist(name) },
            onDismiss = { viewModel.onDismissCreateDialog() }
        )
    }

    // Dialogo borrar playlist
    uiState.playlistToDelete?.let { playlist ->
        DeleteConfirmationDialog(
            playlistName = playlist.name,
            onConfirm = { viewModel.confirmDeletePlaylist() },
            onDismiss = { viewModel.onDismissDeleteDialog() }
        )
    }

    PlaylistsScreenContent(
        uiState = uiState,
        onShowCreateDialog = { viewModel.onShowCreateDialog() },
        onConfirmCreate = { name -> viewModel.createPlaylist(name) },
        onDismissCreate = { viewModel.onDismissCreateDialog() },
        onDeletePlaylistClick = { playlist -> viewModel.onDeletePlaylistClicked(playlist) },
        onConfirmDelete = { viewModel.confirmDeletePlaylist() },
        onDismissDelete = { viewModel.onDismissDeleteDialog() },
        onPlaylistClick = onPlaylistClick
    )
}

// =============================================================
// CONTENIDO DE LA SCREEN
// =============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreenContent(
    uiState: PlaylistsUiState,
    onShowCreateDialog: () -> Unit,
    onConfirmCreate: (String) -> Unit,
    onDismissCreate: () -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onPlaylistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    // 🎨 Fondo oscuro solicitado
    val backgroundColor = Color(0xFF212121)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        containerColor = backgroundColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowCreateDialog,
                containerColor = Color(0xFF00D1A7),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Playlist")
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = Color(0xFF00D1A7))
                }

                uiState.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(uiState.error!!, textAlign = TextAlign.Center, color = Color.White)
                        Button(onClick = { /* retry disabled in preview */ }) {
                            Text("Reintentar")
                        }
                    }
                }

                uiState.playlists.isEmpty() -> {
                    Text(
                        "Aún no has creado ninguna playlist. ¡Crea una con el botón +!",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.playlists) { playlist ->
                            PlaylistItemCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.id) },
                                onDelete = { onDeletePlaylistClick(playlist) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogos
    if (uiState.showCreateDialog) {
        CreatePlaylistDialog(onConfirm = onConfirmCreate, onDismiss = onDismissCreate)
    }

    uiState.playlistToDelete?.let { playlist ->
        DeleteConfirmationDialog(
            playlistName = playlist.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}

// =============================================================
// CARD ESTILO BURBUJA
// =============================================================
@Composable
fun PlaylistItemCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = Color(0xFF00D1A7)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(vertical = 8.dp, horizontal = 14.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(30.dp)) // Burbuja
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(30.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black, // Contraste correcto con el verde

            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar Playlist",
                    tint = Color.Black
                )
            }
        }
    }
}

// =============================================================
// DIALOGOS
// =============================================================
@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nueva Playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nombre de la playlist") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Crear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrar Playlist") },
        text = { Text("¿Estás seguro de que quieres borrar la playlist \"$playlistName\"? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Borrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// =============================================================
// PREVIEW
// =============================================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlaylistsScreenPreview() {

    val fakePlaylists = listOf(
        Playlist(id = 1, name = "Favoritas", songIds = listOf(1, 2)),
        Playlist(id = 2, name = "Para Estudiar", songIds = listOf(5, 7, 9)),
        Playlist(id = 3, name = "Rock", songIds = listOf(4, 6))
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
