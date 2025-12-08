package com.example.integradorasensorproximidad.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa una canción obtenida desde el servidor.
 * El archivo de audio se accederá construyendo una URL completa a partir del `filePath`.
 */
data class NetworkSong(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("artist")
    val artist: String,

    @SerializedName("duration")
    val duration: Long,

    @SerializedName("file_path")
    val filePath: String // Nombre del archivo en el servidor (ej: "cancion1.mp3")
)
