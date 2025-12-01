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
    val showCreateDialog: Boolean = false // Controla la visibilidad del diálogo
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
            _uiState.update { it.copy(isLoading = true, error = null) } // Limpiamos errores anteriores

            val result = repository.getPlaylists()

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        playlists = result.getOrNull() ?: emptyList(),
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

    /**
     * Muestra el diálogo para crear una nueva playlist.
     */
    fun onShowCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    /**
     * Oculta el diálogo para crear una nueva playlist.
     */
    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    /**
     * Llama al repositorio para crear una nueva playlist y recarga la lista si tiene éxito.
     */
    fun createPlaylist(name: String) {
        if (name.isBlank()) return // Evita crear playlists sin nombre

        viewModelScope.launch {
            // Ocultamos el diálogo y mostramos el indicador de carga
            _uiState.update { it.copy(showCreateDialog = false, isLoading = true) }

            val result = repository.createPlaylist(name)

            result.onSuccess {
                // Si la creación fue exitosa, recargamos la lista para ver el nuevo item
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
}
