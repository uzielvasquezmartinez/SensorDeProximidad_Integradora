package com.example.integradorasensorproximidad.ui.player

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    val songToAddToPlaylist: Song? = null,
    val showAddToPlaylistDialog: Boolean = false,
    val availablePlaylists: List<Playlist> = emptyList(),
    val isProximitySensorEnabled: Boolean = false
)

/**
 * ViewModel para la pantalla del reproductor de música.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val repository = MusicRepository()
    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: Job? = null

    // --- Propiedades para el Sensor de Proximidad ---
    private val sensorManager: SensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // Máquina de estados para gestionar los gestos del sensor
    private sealed class ProximityState {
        object Far : ProximityState()
        data class Near(val startTime: Long, val longPressTriggered: Boolean, val wasPlaying: Boolean) : ProximityState()
    }
    private var sensorState: ProximityState = ProximityState.Far
    private var waveCount = 0
    private var waveHandlerJob: Job? = null
    private val LONG_PRESS_THRESHOLD_MS = 500L
    private val WAVE_RESET_TIMEOUT_MS = 700L

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

    // --- Lógica del Sensor de Proximidad ---

    fun enableProximitySensor(enable: Boolean) {
        if (proximitySensor == null) {
            _uiState.update { it.copy(error = "Sensor de proximidad no disponible en este dispositivo.") }
            return
        }

        if (enable) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            sensorManager.unregisterListener(this)
        }
        _uiState.update { it.copy(isProximitySensorEnabled = enable) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return

        val isCurrentlyNear = event.values[0] < (proximitySensor?.maximumRange ?: 5.0f)
        val previousState = sensorState

        if (isCurrentlyNear && previousState is ProximityState.Far) {
            // Estado: Lejos -> Cerca
            val now = System.currentTimeMillis()
            sensorState = ProximityState.Near(startTime = now, longPressTriggered = false, wasPlaying = _uiState.value.isPlaying)
            viewModelScope.launch {
                delay(LONG_PRESS_THRESHOLD_MS)
                // Comprueba si seguimos en el mismo estado "Cerca" tras el retardo
                val stateAfterDelay = sensorState
                if (stateAfterDelay is ProximityState.Near && stateAfterDelay.startTime == now) {
                    // Es un gesto largo
                    if (stateAfterDelay.wasPlaying) {
                        togglePlayPause() // Pausa la música
                    }
                    sensorState = stateAfterDelay.copy(longPressTriggered = true)
                }
            }
        } else if (!isCurrentlyNear && previousState is ProximityState.Near) {
            // Estado: Cerca -> Lejos
            sensorState = ProximityState.Far

            if (previousState.longPressTriggered) {
                // Si se activó el gesto largo, reanudamos la música
                if (previousState.wasPlaying) {
                    togglePlayPause() // Reanuda la música
                }
            } else {
                // Si fue un gesto corto (un "wave")
                waveCount++
                waveHandlerJob?.cancel()
                waveHandlerJob = viewModelScope.launch {
                    delay(WAVE_RESET_TIMEOUT_MS)
                    if (waveCount == 1) {
                        skipNext()
                    } else if (waveCount >= 2) {
                        skipPrevious()
                    }
                    waveCount = 0 // Reinicia el contador tras la acción
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario para este caso de uso.
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
        sensorManager.unregisterListener(this)
        super.onCleared()
    }
}
