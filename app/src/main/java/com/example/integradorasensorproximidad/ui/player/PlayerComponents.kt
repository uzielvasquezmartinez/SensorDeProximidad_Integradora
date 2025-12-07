package com.example.integradorasensorproximidad.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.integradorasensorproximidad.data.model.Song
import java.util.concurrent.TimeUnit

/**
 * Muestra una imagen grande para el arte del álbum (usando un placeholder).
 */
@Composable
fun AlbumArt(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Album Art",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Muestra el título y artista de la canción.
 */
@Composable
fun SongInfo(currentSong: Song?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentSong?.title ?: "Canción Desconocida",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = currentSong?.artist ?: "Artista Desconocido",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Muestra el slider de progreso y los tiempos de la canción.
 * ESTA ES LA FUNCIÓN CORREGIDA.
 */
@Composable
fun SongProgress(
    currentPosition: Long,
    totalDuration: Long,
    onSeekStart: () -> Unit,
    onSeekFinished: (Long) -> Unit
) {
    // Esta variable guarda la posición del slider SOLO mientras el usuario arrastra.
    var sliderPosition by remember { mutableStateOf<Float?>(null) }

    // El valor que se muestra es la posición del arrastre, o si no, la de la canción.
    val displayPosition = sliderPosition ?: currentPosition.toFloat()

    Column {
        Slider(
            value = displayPosition,
            onValueChange = {
                // Cuando el usuario empieza a arrastrar por primera vez
                if (sliderPosition == null) {
                    onSeekStart()
                }
                sliderPosition = it
            },
            onValueChangeFinished = {
                // Cuando el usuario suelta el dedo
                sliderPosition?.let { onSeekFinished(it.toLong()) }
                sliderPosition = null // Reseteamos la posición del arrastre
            },
            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0f),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatDuration(displayPosition.toLong()), style = MaterialTheme.typography.bodySmall)
            Text(text = formatDuration(totalDuration), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Muestra los botones de control del reproductor.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSkipPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                contentDescription = "Reproducir/Pausar",
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


/**
 * Formatea la duración de milisegundos a un formato "mm:ss".
 */
fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}
