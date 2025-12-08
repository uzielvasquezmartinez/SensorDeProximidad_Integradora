package com.example.integradorasensorproximidad.data.network

import com.example.integradorasensorproximidad.data.model.NetworkSong
import com.example.integradorasensorproximidad.data.model.Playlist
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Canciones ---

    @GET("api/songs")
    suspend fun getSongs(): List<NetworkSong>

    @Multipart
    @POST("api/songs")
    suspend fun uploadSong(
        @Part("title") title: RequestBody,
        @Part("artist") artist: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<NetworkSong>

    // --- Playlists ---

    @GET("api/playlists")
    suspend fun getPlaylists(): List<Playlist>

    @POST("api/playlists")
    suspend fun createPlaylist(@Body playlistData: Playlist): Response<Playlist>

    @PUT("api/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: Int,
        @Body playlistData: Playlist
    ): Response<Playlist>

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: Int): Response<ResponseBody>
}
