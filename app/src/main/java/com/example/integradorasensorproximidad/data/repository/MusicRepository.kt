package com.example.integradorasensorproximidad.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.integradorasensorproximidad.data.model.NetworkSong
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
    // NOTA: En una app de producción, esta URL base se inyectaría, no se codificaría aquí.
    private val baseUrl = "http://192.168.56.1:5000/"

    // --- MÉTODOS DE RED ---

    /**
     * Obtiene la lista de canciones desde el servidor y las mapea al modelo Song unificado.
     */
    suspend fun getNetworkSongs(): Result<List<Song>> {
        return try {
            val networkSongs = apiService.getSongs()
            // Mapea la respuesta de la red (NetworkSong) al modelo de dominio (Song)
            val songs = networkSongs.map { networkSong ->
                Song(
                    id = networkSong.id,
                    title = networkSong.title,
                    artist = networkSong.artist,
                    duration = networkSong.duration,
                    contentUri = null, // Es una canción de red, no tiene URI local
                    networkUrl = baseUrl + "api/audio/" + networkSong.filePath
                )
            }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene las playlists desde la API remota.
     */
    suspend fun getNetworkPlaylists(): Result<List<Playlist>> {
        return try {
            val playlists = apiService.getPlaylists()
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva playlist en la API remota.
     */
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

    /**
     * Sube una canción local al servidor.
     */
    suspend fun uploadSong(context: Context, song: Song): Result<Song> {
        // Una canción debe ser local (tener contentUri) para poder ser subida.
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
                MultipartBody.Part.createFormData("song_file", song.title, requestFile)
            } ?: return Result.failure(Exception("No se pudo leer el archivo local para subirlo."))

            val response = apiService.uploadSong(title, artist, duration, filePart)
            if (response.isSuccessful && response.body() != null) {
                val networkSong = response.body()!!
                // Mapea la respuesta a nuestro modelo de dominio unificado
                val newSong = Song(
                    id = networkSong.id,
                    title = networkSong.title,
                    artist = networkSong.artist,
                    duration = networkSong.duration,
                    networkUrl = baseUrl + "api/audio/" + networkSong.filePath
                )
                Result.success(newSong)
            } else {
                Result.failure(Exception("Error al subir la canción: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MÉTODO DE DATOS LOCALES ---

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

                // Crea la instancia de Song para una canción local
                songList.add(Song(id = id, title = title, artist = artist, duration = duration, contentUri = contentUri))
            }
        }

        return songList
    }
}
