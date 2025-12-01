package com.example.integradorasensorproximidad.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.integradorasensorproximidad.data.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel(),
    onPlaylistClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Diálogo para crear playlist
    if (uiState.showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name -> viewModel.createPlaylist(name) },
            onDismiss = { viewModel.onDismissCreateDialog() }
        )
    }

    // Diálogo para confirmar borrado
    uiState.playlistToDelete?.let { playlist ->
        DeleteConfirmationDialog(
            playlistName = playlist.name,
            onConfirm = { viewModel.confirmDeletePlaylist() },
            onDismiss = { viewModel.onDismissDeleteDialog() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Crear Playlist")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(uiState.error!!, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadPlaylists() }) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.playlists.isEmpty() && !uiState.isLoading -> {
                    Text("Aún no has creado ninguna playlist. ¡Crea una con el botón +!", textAlign = TextAlign.Center)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        items(uiState.playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlaylistClick(playlist.id) }
                                        .padding(start = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = playlist.name,
                                        modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = { viewModel.onDeletePlaylistClicked(playlist) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Borrar Playlist"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nueva Playlist") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Nombre de la playlist") }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Crear") } },
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
            ) {
                Text("Borrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
