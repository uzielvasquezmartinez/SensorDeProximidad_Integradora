package com.example.integradorasensorproximidad.data.network

import com.example.integradorasensorproximidad.data.model.Playlist
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define los endpoints de la API para el CRUD de Playlists.
 */
interface ApiService {

    // CREATE
    @POST("playlists")
    suspend fun createPlaylist(@Body playlist: Playlist): Response<Playlist>

    // READ (all)
    @GET("playlists")
    suspend fun getAllPlaylists(): Response<List<Playlist>>

    // READ (one by id)
    @GET("playlists/{id}")

    suspend fun getPlaylistById(@Path("id") id: Int): Response<Playlist>
    // UPDATE
    @PUT("playlists/{id}")
    suspend fun updatePlaylist(@Path("id") id: Int, @Body playlist: Playlist): Response<Playlist>

    // DELETE
    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Int): Response<Unit>

}
