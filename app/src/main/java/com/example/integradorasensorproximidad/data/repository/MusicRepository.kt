package com.example.integradorasensorproximidad.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import com.example.integradorasensorproximidad.data.network.ApiService
import com.example.integradorasensorproximidad.data.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repositorio que gestiona la obtención de datos de música, tanto locales como remotos.
 */
class MusicRepository {

    private val apiService: ApiService = RetrofitClient.apiService
    // CORRECCIÓN: Obtenemos la URL base directamente del cliente de Retrofit para evitar inconsistencias.
    private val baseUrl = RetrofitClient.BASE_URL

    // --- MÉTODOS DE RED PARA PLAYLISTS ---

    suspend fun getNetworkPlaylists(): Result<List<Playlist>> {
        return try {
            val playlists = apiService.getPlaylists()
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNetworkPlaylist(playlist: Playlist): Result<Playlist> {
        return try {
            val response = apiService.createPlaylist(playlist)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear la playlist: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNetworkPlaylist(playlist: Playlist): Result<Playlist> {
        return try {
            val response = apiService.updatePlaylist(playlist.id, playlist)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar la playlist: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNetworkPlaylist(playlistId: Int): Result<Unit> {
        return try {
            val response = apiService.deletePlaylist(playlistId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al borrar la playlist: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MÉTODOS DE RED PARA CANCIONES ---

    suspend fun getNetworkSongs(): Result<List<Song>> {
        return try {
            val networkSongs = apiService.getSongs()
            val songs = networkSongs.map { networkSong ->
                Song(
                    id = networkSong.id,
                    title = networkSong.title,
                    artist = networkSong.artist,
                    duration = networkSong.duration,
                    contentUri = null,
                    // La URL se construye correctamente usando la baseUrl centralizada
                    networkUrl = "${baseUrl}api/audio/${networkSong.filePath}"
                )
            }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadSong(context: Context, song: Song): Result<Song> {
        val localUri = song.contentUri ?: return Result.failure(Exception("La canción a subir debe ser un archivo local."))

        return try {
            val title = song.title.toRequestBody(MultipartBody.FORM)
            val artist = song.artist.toRequestBody(MultipartBody.FORM)
            val duration = song.duration.toString().toRequestBody(MultipartBody.FORM)

            val filePart = context.contentResolver.openInputStream(localUri)?.use { inputStream ->
                val fileBytes = inputStream.readBytes()
                val requestFile = fileBytes.toRequestBody(
                    context.contentResolver.getType(localUri)?.toMediaTypeOrNull()
                )
                // CORRECCIÓN: El nombre del campo debe ser "file" para coincidir con la API de Python.
                MultipartBody.Part.createFormData("file", song.title, requestFile)
            } ?: return Result.failure(Exception("No se pudo leer el archivo local para subirlo."))

            val response = apiService.uploadSong(title, artist, duration, filePart)
            if (response.isSuccessful && response.body() != null) {
                val networkSong = response.body()!!
                val newSong = Song(
                    id = networkSong.id,
                    title = networkSong.title,
                    artist = networkSong.artist,
                    duration = networkSong.duration,
                    networkUrl = "${baseUrl}api/audio/${networkSong.filePath}"
                )
                Result.success(newSong)
            } else {
                Result.failure(Exception("Error al subir la canción (Code: ${response.code()}): ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MÉTODO DE DATOS LOCALES ---

    fun getLocalSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null
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
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                songList.add(Song(id = id, title = title, artist = artist, duration = duration, contentUri = contentUri))
            }
        }
        return songList
    }
}
