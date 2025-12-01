package com.example.integradorasensorproximidad.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa una lista de reproducción guardada en el servidor.
 */
data class Playlist(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("song_ids")
    val songIds: List<Long> // Lista de los IDs de las canciones locales
)
