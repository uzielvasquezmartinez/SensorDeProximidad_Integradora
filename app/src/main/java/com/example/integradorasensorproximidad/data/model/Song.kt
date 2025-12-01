package com.example.integradorasensorproximidad.data.model

import android.net.Uri

/**
 * Representa un único archivo de audio encontrado en el dispositivo del usuario.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long, // en milisegundos
    val contentUri: Uri // La 'dirección' para acceder al archivo
)
