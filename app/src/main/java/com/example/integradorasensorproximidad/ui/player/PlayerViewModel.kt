package com.example.integradorasensorproximidad.ui.player

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado que representa la UI de la pantalla del reproductor.
 */
data class PlayerUiState(
    val songList: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val totalDuration: Long = 0,
    val permissionGranted: Boolean = false,
    val error: String? = null,

    // Estado para el diálogo de "Añadir a Playlist"
    val songToAddToPlaylist: Song? = null,
    val showAddToPlaylistDialog: Boolean = false,
    val availablePlaylists: List<Playlist> = emptyList()
)

/**
 * ViewModel para la pantalla del reproductor de música.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository()
    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // --- Lógica de Permisos y Carga de Canciones ---
    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(permissionGranted = isGranted, error = null) }
        if (isGranted) {
            loadNetworkSongs()
        }
    }

    private fun loadLocalSongs() {
        viewModelScope.launch {
            val songs = repository.getLocalSongs(getApplication())
            _uiState.update { it.copy(songList = songs) }
            if (songs.isNotEmpty() && _uiState.value.currentSong == null) {
                prepareSong(songs.first())
            }
        }
    }

    private fun loadNetworkSongs() {
        viewModelScope.launch {
            val result = repository.getNetworkSongs()
            result.onSuccess {
                _uiState.update { state -> state.copy(songList = it) }
                if (it.isNotEmpty() && _uiState.value.currentSong == null) {
                    prepareSong(it.first())
                }
            }.onFailure {
                _uiState.update { state -> state.copy(error = "Error al cargar canciones de la red: ${it.message}") }
                loadLocalSongs()
            }
        }
    }

    // --- Lógica de Reproducción ---
    private fun prepareSong(song: Song) {
        _uiState.update { it.copy(currentSong = song, isPlaying = false, currentPosition = 0) }
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                when {
                    song.networkUrl != null -> setDataSource(song.networkUrl)
                    song.contentUri != null -> setDataSource(getApplication(), song.contentUri)
                    else -> {
                        _uiState.update { it.copy(error = "La canción no tiene una fuente válida (ni red ni local).") }
                        return
                    }
                }
                prepareAsync()
                setOnPreparedListener { mp -> _uiState.update { it.copy(totalDuration = mp.duration.toLong()) } }
                setOnCompletionListener { skipNext() }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al preparar la canción: ${e.message}") }
            }
        }
    }

    fun playSong(song: Song) {
        mediaPlayer?.release()
        _uiState.update { it.copy(currentSong = song, isPlaying = true, currentPosition = 0) }
        mediaPlayer = MediaPlayer().apply {
            try {
                when {
                    song.networkUrl != null -> setDataSource(song.networkUrl)
                    song.contentUri != null -> setDataSource(getApplication(), song.contentUri)
                    else -> {
                        _uiState.update { it.copy(error = "La canción no tiene una fuente válida.") }
                        return
                    }
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    _uiState.update { it.copy(totalDuration = mp.duration.toLong()) }
                    mp.start()
                    startProgressUpdates()
                }
                setOnCompletionListener { skipNext() }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al reproducir la canción: ${e.message}") }
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                stopProgressUpdates()
                _uiState.update { state -> state.copy(isPlaying = false) }
            } else {
                it.start()
                startProgressUpdates()
                _uiState.update { state -> state.copy(isPlaying = true) }
            }
        }
    }

    fun skipNext() {
        val currentState = _uiState.value
        if (currentState.songList.isEmpty()) return
        val currentIndex = currentState.songList.indexOf(currentState.currentSong)
        val nextIndex = if (currentIndex == currentState.songList.size - 1) 0 else currentIndex + 1
        playSong(currentState.songList[nextIndex])
    }

    fun skipPrevious() {
        val currentState = _uiState.value
        if (currentState.songList.isEmpty()) return
        val currentIndex = currentState.songList.indexOf(currentState.currentSong)
        val prevIndex = if (currentIndex == 0) currentState.songList.size - 1 else currentIndex - 1
        playSong(currentState.songList[prevIndex])
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _uiState.update { it.copy(currentPosition = position) }
    }

    // --- Lógica de "Añadir a Playlist" ---

    fun onAddSongClicked(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(songToAddToPlaylist = song, error = null) }
            val result = repository.getNetworkPlaylists()
            result.onSuccess { playlists ->
                _uiState.update {
                    it.copy(
                        availablePlaylists = playlists,
                        showAddToPlaylistDialog = true
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(error = "No se pudieron cargar las playlists.") }
            }
        }
    }

    fun onDismissAddToPlaylistDialog() {
        _uiState.update {
            it.copy(
                showAddToPlaylistDialog = false,
                songToAddToPlaylist = null,
                availablePlaylists = emptyList()
            )
        }
    }

    fun addSongToPlaylist(playlist: Playlist) {
        val songToAdd = _uiState.value.songToAddToPlaylist ?: return

        viewModelScope.launch {
            val songIdToAdd: Long

            // Paso 1: Diferenciar entre canción local y de red
            val isLocalSong = songToAdd.contentUri != null && songToAdd.networkUrl == null

            if (isLocalSong) {
                // Es una canción local, necesita ser subida primero.
                _uiState.update { it.copy(error = "Subiendo canción al servidor...") }
                val uploadResult = repository.uploadSong(getApplication(), songToAdd)

                // Usamos getOrNull para obtener el ID de forma segura. Si falla, es null.
                val uploadedSongId = uploadResult.getOrNull()?.id

                if (uploadedSongId == null) {
                    // La subida falló. Mostramos el error y detenemos el proceso.
                    uploadResult.onFailure { exception ->
                        _uiState.update { it.copy(error = "Error al subir la canción: ${exception.message}") }
                    }
                    onDismissAddToPlaylistDialog()
                    return@launch
                }
                songIdToAdd = uploadedSongId
            } else {
                // Es una canción de red, usamos su ID directamente.
                songIdToAdd = songToAdd.id
            }

            // Paso 2: Con un ID de red válido, actualizar la playlist.
            if (playlist.songIds.contains(songIdToAdd)) {
                _uiState.update { it.copy(error = "La canción ya está en esta playlist.") }
                onDismissAddToPlaylistDialog()
                return@launch
            }

            val updatedSongIds = playlist.songIds + songIdToAdd
            val updatedPlaylist = playlist.copy(songIds = updatedSongIds)

            val result = repository.updateNetworkPlaylist(updatedPlaylist)

            result.onFailure { exception ->
                _uiState.update { state ->
                    state.copy(error = "Error al actualizar la playlist: ${exception.message}")
                }
            }

            onDismissAddToPlaylistDialog()
        }
    }

    // --- Gestión del ciclo de vida ---

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                try {
                    _uiState.update { it.copy(currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0) }
                } catch (e: IllegalStateException) {
                    break
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
    }

    override fun onCleared() {
        mediaPlayer?.release()
        stopProgressUpdates()
        super.onCleared()
    }
}
