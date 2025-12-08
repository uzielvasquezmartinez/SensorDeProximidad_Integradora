package com.example.integradorasensorproximidad.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado que representa la UI de la pantalla de Playlists.
 */
data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val playlistToDelete: Playlist? = null // Guarda la playlist a borrar para mostrar el diálogo
)

class PlaylistsViewModel : ViewModel() {

    private val repository = MusicRepository()

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    /**
     * Carga las playlists desde el repositorio.
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getNetworkPlaylists()

            result.onSuccess { playlists ->
                _uiState.update {
                    it.copy(
                        playlists = playlists,
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        error = "No se pudieron cargar las playlists. Verifica tu conexión.",
                        isLoading = false
                    )
                }
            }
        }
    }

    // --- Lógica de Creación ---
    fun onShowCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(showCreateDialog = false, isLoading = true) }

            val newPlaylist = Playlist(id = 0, name = name, songIds = emptyList())
            val result = repository.createNetworkPlaylist(newPlaylist)

            result.onSuccess {
                loadPlaylists()
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        error = "Error al crear la playlist: ${exception.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    // --- Lógica de Borrado ---
    fun onDeletePlaylistClicked(playlist: Playlist) {
        _uiState.update { it.copy(playlistToDelete = playlist) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(playlistToDelete = null) }
    }

    fun confirmDeletePlaylist() {
        val playlist = _uiState.value.playlistToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, playlistToDelete = null) }

            val result = repository.deleteNetworkPlaylist(playlist.id)

            result.onSuccess {
                loadPlaylists() // Recargamos la lista para que desaparezca la borrada
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        error = "Error al borrar la playlist: ${exception.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}
