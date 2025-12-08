package com.example.integradorasensorproximidad.ui.playlists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.data.repository.MusicRepository
import com.example.integradorasensorproximidad.ui.navigation.PLAYLIST_ID_ARG
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

            // Hacemos dos llamadas a la red en paralelo para más eficiencia
            val (playlistsResult, networkSongsResult) = coroutineScope {
                val playlists = async { repository.getNetworkPlaylists() }
                val songs = async { repository.getNetworkSongs() }
                playlists.await() to songs.await()
            }

            // Procesamos los resultados
            playlistsResult.onSuccess { allPlaylists ->
                val playlist = allPlaylists.find { it.id == playlistId }

                if (playlist != null) {
                    networkSongsResult.onSuccess { allNetworkSongs ->
                        // Filtramos las canciones de la RED que están en la playlist
                        val songsInPlaylist = allNetworkSongs.filter { networkSong ->
                            playlist.songIds.contains(networkSong.id)
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
                            it.copy(error = "Error al cargar las canciones de la red.", isLoading = false)
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "No se encontró la playlist con ID $playlistId.", isLoading = false)
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(error = "Error al cargar las playlists.", isLoading = false)
                }
            }
        }
    }
}
