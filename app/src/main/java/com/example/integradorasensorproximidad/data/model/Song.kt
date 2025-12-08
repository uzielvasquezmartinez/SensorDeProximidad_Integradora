package com.example.integradorasensorproximidad.data.model

import android.net.Uri

/**
 * Representa una canción, que puede ser local o de red.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,

    // Para canciones locales, esta será la URI del Content Provider.
    val contentUri: Uri? = null,

    // Para canciones de red, esta será la URL completa para el streaming.
    val networkUrl: String? = null
) {
    // Propiedad para saber fácilmente si la canción es de red o no.
    val isFromNetwork: Boolean
        get() = networkUrl != null
}
