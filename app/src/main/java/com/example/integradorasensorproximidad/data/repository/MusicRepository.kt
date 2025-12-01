package com.example.integradorasensorproximidad.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.data.network.RetrofitClient

/**
 * Repositorio que gestiona la obtención de datos de música, tanto locales como remotos.
 */
class MusicRepository {

    /**
     * Obtiene las playlists desde la API remota.
     */
    suspend fun getPlaylists(): Result<List<Playlist>> {
        return try {
            val response = RetrofitClient.apiService.getAllPlaylists()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al obtener las playlists: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los detalles de una playlist específica desde la API.
     */
    suspend fun getPlaylistById(id: Int): Result<Playlist> {
        return try {
            val response = RetrofitClient.apiService.getPlaylistById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener la playlist: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva playlist en la API remota.
     */
    suspend fun createPlaylist(name: String): Result<Playlist> {
        return try {
            val newPlaylist = Playlist(id = 0, name = name, songIds = emptyList())
            val response = RetrofitClient.apiService.createPlaylist(newPlaylist)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear la playlist: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza una playlist existente en la API remota.
     */
    suspend fun updatePlaylist(playlist: Playlist): Result<Playlist> {
        return try {
            val response = RetrofitClient.apiService.updatePlaylist(playlist.id, playlist)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar la playlist: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escanea el almacenamiento compartido del dispositivo en busca de archivos de audio.
     */
    fun getLocalSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        // Consulta solo archivos de música y que duren al menos 30 segundos
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                songList.add(Song(id, title, artist, duration, contentUri))
            }
        }

        return songList
    }
}
