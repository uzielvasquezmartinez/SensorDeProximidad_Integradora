package com.example.integradorasensorproximidad.ui.player

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.ui.viewmodel.PlayerViewModel
import com.example.integradorasensorproximidad.ui.viewmodel.PlayerUiState

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = uiState.availablePlaylists,
            onPlaylistSelected = { playlist -> viewModel.addSongToPlaylist(playlist) },
            onDismiss = { viewModel.onDismissAddToPlaylistDialog() }
        )
    }

    uiState.error?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> viewModel.onPermissionResult(isGranted) }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.permissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // --- Sección del Reproductor ---
                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    AlbumArt(modifier = Modifier.weight(1f).padding(32.dp))
                    SongInfo(currentSong = uiState.currentSong)
                    Spacer(modifier = Modifier.height(16.dp))
                    SongProgress(
                        currentPosition = uiState.currentPosition,
                        totalDuration = uiState.totalDuration,
                        onSeekStart = { },
                        onSeekFinished = { position -> viewModel.seekTo(position) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PlayerControls(
                        isPlaying = uiState.isPlaying,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onSkipPrevious = { viewModel.skipPrevious() }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- NUEVO CONTROL DEL SENSOR ---
                    ProximitySensorControl(
                        isSensorEnabled = uiState.isProximitySensorEnabled,
                        onToggleSensor = { enable -> viewModel.enableProximitySensor(enable) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Sección de la Lista ---
                Column(modifier = Modifier.weight(1f)) {
                    Text("Playlist", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(uiState.songList) { song ->
                            val isCurrentSong = uiState.currentSong?.id == song.id
                            val backgroundColor = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            val textColor = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${song.title} - ${song.artist}",
                                    color = textColor,
                                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.playSong(song) }
                                        .padding(vertical = 12.dp)
                                )
                                IconButton(onClick = { viewModel.onAddSongClicked(song) }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Añadir a Playlist",
                                        tint = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "Se necesita permiso para acceder a la música. Por favor, otórguelo en la configuración de la aplicación.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onPlaylistSelected: (Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir canción a...") },
        text = {
            if (playlists.isEmpty()) {
                Text("Primero debes crear una playlist.")
            } else {
                LazyColumn {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
