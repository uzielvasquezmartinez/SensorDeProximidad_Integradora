package com.example.integradorasensorproximidad.ui.playlists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.data.repository.MusicRepository
import com.example.integradorasensorproximidad.ui.navigation.PLAYLIST_ID_ARG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado que representa la UI de la pantalla de Detalle de Playlist.
 */
data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PlaylistDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = MusicRepository()
    private val playlistId: Int = savedStateHandle.get<Int>(PLAYLIST_ID_ARG) ?: 0

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        if (playlistId > 0) {
            loadPlaylistDetails()
        } else {
            _uiState.update { it.copy(error = "ID de playlist inválido.") }
        }
    }

    fun loadPlaylistDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Obtener los detalles de la playlist desde la API
            val playlistResult = repository.getPlaylistById(playlistId)

            playlistResult.onSuccess { playlist ->
                // 2. Obtener TODAS las canciones locales
                val allLocalSongs = repository.getLocalSongs(getApplication())

                // 3. Filtrar las canciones locales para quedarnos solo con las de la playlist
                val songsInPlaylist = allLocalSongs.filter { song ->
                    playlist.songIds.contains(song.id)
                }

                _uiState.update {
                    it.copy(
                        playlist = playlist,
                        songs = songsInPlaylist,
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        error = "No se pudieron cargar los detalles de la playlist.",
                        isLoading = false
                    )
                }
            }
        }
    }
}
