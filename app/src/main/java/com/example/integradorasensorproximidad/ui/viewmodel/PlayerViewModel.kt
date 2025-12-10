package com.example.integradorasensorproximidad.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.data.repository.MusicRepository
import com.example.integradorasensorproximidad.util.ProximitySensorHelper
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val songToAddToPlaylist: Song? = null,
    val showAddToPlaylistDialog: Boolean = false,
    val availablePlaylists: List<Playlist> = emptyList(),
    val isProximitySensorEnabled: Boolean = false
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

    private val sensorHelper = ProximitySensorHelper(
        context = getApplication(),
        coroutineScope = viewModelScope,
        isPlayingProvider = { _uiState.value.isPlaying },
        onTogglePlayPause = ::togglePlayPause,
        onSkipNext = ::skipNext,
        onSkipPrevious = ::skipPrevious
    )

    // --- Lógica de Permisos y Carga de Canciones ---
    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(permissionGranted = isGranted, error = null) }
        if (isGranted) {
            loadAllSongs()
        }
    }

    private fun loadAllSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val networkResult = repository.getNetworkSongs()
            val localSongs = repository.getLocalSongs(getApplication())

            networkResult.onSuccess { networkSongs ->
                val combinedList = (networkSongs + localSongs).distinctBy { it.title to it.artist }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        songList = combinedList,
                        error = null
                    )
                }
                if (combinedList.isNotEmpty() && _uiState.value.currentSong == null) {
                    prepareSong(combinedList.first())
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        songList = localSongs,
                        error = "Error de red. Mostrando solo canciones locales."
                    )
                }
                if (localSongs.isNotEmpty() && _uiState.value.currentSong == null) {
                    prepareSong(localSongs.first())
                }
            }
        }
    }


    // --- Lógica de Reproducción ---
    private fun prepareSong(song: Song) {
        // Actualizamos el estado para reflejar la nueva canción, pero aún no está sonando.
        _uiState.update { it.copy(currentSong = song, isPlaying = false, currentPosition = 0, totalDuration = 0) }
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
                // Preparamos el reproductor de forma asíncrona
                prepareAsync()
                // Cuando esté listo, actualizamos la duración total
                setOnPreparedListener { mp -> 
                    _uiState.update { it.copy(totalDuration = mp.duration.toLong()) } 
                }
                setOnCompletionListener { skipNext() }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al preparar la canción: ${e.message}") }
            }
        }
    }

    // ¡FUNCIÓN CORREGIDA!
    fun playSong(song: Song) {
        // Detenemos cualquier actualización de progreso anterior
        stopProgressUpdates()
        // Liberamos el reproductor anterior para evitar solapamientos
        mediaPlayer?.release()

        // Actualizamos la UI inmediatamente para que el usuario vea la selección
        _uiState.update { it.copy(currentSong = song, isPlaying = true, currentPosition = 0, totalDuration = 0) }

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
                // El punto clave: la reproducción SÓLO comienza cuando el reproductor está listo.
                setOnPreparedListener { mp ->
                    _uiState.update { it.copy(totalDuration = mp.duration.toLong()) }
                    mp.start() // <-- INICIAR REPRODUCCIÓN AQUÍ
                    startProgressUpdates() // Empezar a actualizar la barra de progreso
                }
                // Preparamos el reproductor. El listener anterior se encargará de iniciarlo.
                prepareAsync()

                setOnCompletionListener { skipNext() }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al reproducir la canción: ${e.message}") }
                _uiState.update { it.copy(isPlaying = false) } // Aseguramos que el estado de reproducción sea consistente
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
                // Solo inicia si el reproductor está preparado
                if (_uiState.value.totalDuration > 0) {
                    it.start()
                    startProgressUpdates()
                    _uiState.update { state -> state.copy(isPlaying = true) }
                }
            }
        }
    }

    fun skipNext() {
        val currentState = _uiState.value
        if (currentState.songList.isEmpty()) return
        val currentIndex = currentState.songList.indexOf(currentState.currentSong)
        if (currentIndex == -1 && currentState.songList.isNotEmpty()) {
             playSong(currentState.songList.first()) // Si no hay canción actual, reproducir la primera
             return
        }
        val nextIndex = if (currentIndex >= currentState.songList.size - 1) 0 else currentIndex + 1
        playSong(currentState.songList[nextIndex])
    }

    fun skipPrevious() {
        val currentState = _uiState.value
        if (currentState.songList.isEmpty()) return
        val currentIndex = currentState.songList.indexOf(currentState.currentSong)
        if (currentIndex == -1 && currentState.songList.isNotEmpty()) {
             playSong(currentState.songList.first()) // Si no hay canción actual, reproducir la primera
             return
        }
        val prevIndex = if (currentIndex <= 0) currentState.songList.size - 1 else currentIndex - 1
        playSong(currentState.songList[prevIndex])
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _uiState.update { it.copy(currentPosition = position) }
    }

    // --- Lógica del Sensor de Proximidad (Delegada) ---

    fun enableProximitySensor(enable: Boolean) {
        if (!sensorHelper.isSensorAvailable) {
            _uiState.update { it.copy(error = "Sensor de proximidad no disponible en este dispositivo.") }
            return
        }

        if (enable) {
            sensorHelper.startListening()
        } else {
            sensorHelper.stopListening()
        }
        _uiState.update { it.copy(isProximitySensorEnabled = enable) }
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

            val isLocalSong = songToAdd.contentUri != null && songToAdd.networkUrl == null

            if (isLocalSong) {
                _uiState.update { it.copy(error = "Subiendo canción al servidor...") }
                val uploadResult = repository.uploadSong(getApplication(), songToAdd)
                val uploadedSongId = uploadResult.getOrNull()?.id

                if (uploadedSongId == null) {
                    uploadResult.onFailure { exception ->
                        _uiState.update { it.copy(error = "Error al subir la canción: ${exception.message}") }
                    }
                    onDismissAddToPlaylistDialog()
                    return@launch
                }
                songIdToAdd = uploadedSongId
            } else {
                songIdToAdd = songToAdd.id
            }

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
        sensorHelper.stopListening()
        super.onCleared()
    }
}
