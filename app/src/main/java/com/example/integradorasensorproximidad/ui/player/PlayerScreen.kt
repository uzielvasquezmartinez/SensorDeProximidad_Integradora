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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
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

    PlayerScreenContent(
        uiState = uiState,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSkipNext = { viewModel.skipNext() },
        onSkipPrevious = { viewModel.skipPrevious() },
        onSeekTo = { pos -> viewModel.seekTo(pos) },
        onPlaySong = { song -> viewModel.playSong(song) },
        onAddSongClicked = { song -> viewModel.onAddSongClicked(song) },
        onAddSongToPlaylist = { playlist -> viewModel.addSongToPlaylist(playlist) },
        onDismissAddToPlaylistDialog = { viewModel.onDismissAddToPlaylistDialog() },
        onToggleProximitySensor = { enabled -> viewModel.enableProximitySensor(enabled) }
    )


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

@Composable
fun PlayerScreenContent(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlaySong: (Song) -> Unit,
    onAddSongClicked: (Song) -> Unit,
    onAddSongToPlaylist: (Playlist) -> Unit,
    onDismissAddToPlaylistDialog: () -> Unit,
    onToggleProximitySensor: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isTablet = screenWidthDp >= 700   // ← punto de quiebre para mostrar 2 columnas

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF212121)),
        contentAlignment = Alignment.Center
    ) {

        if (uiState.permissionGranted) {

            if (isTablet) {
                // ----------------------------------------------------------
                //  TABLET → 2 columnas: reproductor | lista canciones
                // ----------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Columna izquierda → Reproductor
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {

                        AlbumArt()
                        Spacer(modifier = Modifier.height(16.dp))
                        SongInfo(uiState.currentSong)
                        Spacer(modifier = Modifier.height(10.dp))

                        ProximitySensorControl(
                            isSensorEnabled = uiState.isProximitySensorEnabled,
                            onToggleSensor = onToggleProximitySensor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SongProgress(
                            currentPosition = uiState.currentPosition,
                            totalDuration = uiState.totalDuration,
                            onSeekStart = {},
                            onSeekFinished = onSeekTo
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PlayerControls(
                            isPlaying = uiState.isPlaying,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious
                        )
                    }

                    // Columna derecha → Lista de canciones
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        items(uiState.songList) { song ->

                            val isCurrent = uiState.currentSong?.id == song.id

                            val bubbleColor = if (isCurrent)
                                Color(0xFF00D1A7)
                            else
                                Color(0xFF00D1A7).copy(alpha = 0.45f)

                            val textColor = if (isCurrent)
                                Color.Black
                            else
                                Color.Black.copy(alpha = 0.8f)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(65.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { onPlaySong(song) },
                                colors = CardDefaults.cardColors(
                                    containerColor = bubbleColor
                                ),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${song.title} - ${song.artist}",
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = { onAddSongClicked(song) }) {
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
                // ----------------------------------------------------------
                //  TELÉFONO → diseño actual (no se toca)
                // ----------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        AlbumArt()
                        Spacer(modifier = Modifier.height(16.dp))
                        SongInfo(uiState.currentSong)
                        Spacer(modifier = Modifier.height(10.dp))

                        ProximitySensorControl(
                            isSensorEnabled = uiState.isProximitySensorEnabled,
                            onToggleSensor = onToggleProximitySensor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SongProgress(
                            currentPosition = uiState.currentPosition,
                            totalDuration = uiState.totalDuration,
                            onSeekStart = {},
                            onSeekFinished = onSeekTo
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PlayerControls(
                            isPlaying = uiState.isPlaying,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious
                        )
                    }

                    // Lista canciones
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.songList) { song ->

                            val isCurrent = uiState.currentSong?.id == song.id

                            val bubbleColor = if (isCurrent)
                                Color(0xFF00D1A7)
                            else
                                Color(0xFF00D1A7).copy(alpha = 0.45f)

                            val textColor = if (isCurrent)
                                Color.Black
                            else
                                Color.Black.copy(alpha = 0.8f)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(65.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { onPlaySong(song) },
                                colors = CardDefaults.cardColors(
                                    containerColor = bubbleColor
                                ),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${song.title} - ${song.artist}",
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = { onAddSongClicked(song) }) {
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
            }

        } else {
            Text(
                "Se necesita permiso para acceder a la música. Por favor, otórguelo en ajustes.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlayerScreenContentPreview() {
    val fakeSongs = listOf(
        Song(1, "Nightfall Memories", "Echo Dreams", 210000L),
        Song(2, "Crystal Skies", "Luna Waves", 185000L),
        Song(3, "Sunset Drive", "Neon Runner", 200000L)
    )

    val fakeUiState = PlayerUiState(
        permissionGranted = true,
        isPlaying = true,
        currentSong = fakeSongs[0],
        currentPosition = 45_000L,
        totalDuration = fakeSongs[0].duration,
        songList = fakeSongs,
        showAddToPlaylistDialog = false,
        availablePlaylists = listOf(Playlist(1, "Favoritos", listOf(1,2,3))),
        isProximitySensorEnabled = true,
        error = null
    )

    MaterialTheme {
        PlayerScreenContent(
            uiState = fakeUiState,
            onTogglePlayPause = {},
            onSkipNext = {},
            onSkipPrevious = {},
            onSeekTo = {},
            onPlaySong = {},
            onAddSongClicked = {},
            onAddSongToPlaylist = {},
            onDismissAddToPlaylistDialog = {},
            onToggleProximitySensor = {}
        )
    }
}

