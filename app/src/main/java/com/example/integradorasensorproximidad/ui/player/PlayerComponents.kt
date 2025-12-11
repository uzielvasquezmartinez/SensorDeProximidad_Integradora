package com.example.integradorasensorproximidad.ui.player
import adaptiveDp
import android.graphics.RenderEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.integradorasensorproximidad.data.model.Playlist
import com.example.integradorasensorproximidad.data.model.Song
import java.util.concurrent.TimeUnit
import kotlin.text.toLong
import kotlin.time.Duration.Companion.seconds

/**
 * Muestra una imagen grande para el arte del álbum (usando un placeholder).
 */
@Composable
fun AlbumArt(
    modifier: Modifier = Modifier,
    artSize: Dp,
    iconSize: Dp
) {

    val accent = Color(0xFF00D1A7)

    Box(
        modifier = modifier
            .size(artSize)
            .shadow(
                elevation = 25.dp,
                shape = CircleShape,
                ambientColor = accent.copy(alpha = 0.55f),
                spotColor = accent.copy(alpha = 0.30f)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.35f),
                        accent.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
            .padding(10.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.45f),
                        accent.copy(alpha = 0.25f)
                    )
                ),
                shape = CircleShape
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Album Art",
            modifier = Modifier.size(iconSize),
            tint = Color.White.copy(alpha = 0.75f)
        )
    }
}


/**
 * Muestra el título y artista de la canción.
 */
@Composable
fun SongInfo(currentSong: Song?) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // opcional, para que no toque los bordes
    ) {
        // Calcular tamaños de fuente según el ancho disponible
        val titleFontSize = (maxWidth.value / 10).sp
        val artistFontSize = (maxWidth.value / 15).sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentSong?.title ?: "Canción Desconocida",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center // asegura centrado
            )
            Text(
                text = currentSong?.artist ?: "Artista Desconocido",
                fontSize = artistFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center // asegura centrado
            )
        }
    }
}


/**
 * Muestra el slider de progreso y los tiempos de la canción.
 */
@Composable

fun SongProgress(

    currentPosition: Long,

    totalDuration: Long,

    onSeekStart: () -> Unit,

    onSeekFinished: (Long) -> Unit

) {

// Detecta el tamaño de pantalla

    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp

// Tamaño responsivo del texto

    val timeFontSize = when {

        screenWidth < 360 -> 10.sp

        screenWidth < 400 -> 12.sp

        screenWidth < 500 -> 14.sp

        else -> 16.sp

    }

// Grosor de la barra según pantalla

    val sliderHeight = when {

        screenWidth < 360 -> 2.dp

        screenWidth < 400 -> 3.dp

        else -> 4.dp

    }

// Padding responsivo

    val horizontalPadding = when {

        screenWidth < 360 -> 8.dp

        screenWidth < 400 -> 12.dp

        else -> 16.dp

    }

    var sliderPosition by remember { mutableStateOf<Float?>(null) }

    val displayPosition = sliderPosition ?: currentPosition.toFloat()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontalPadding)) {

        Slider(

            value = displayPosition,

            onValueChange = {

                if (sliderPosition == null) {

                    onSeekStart()

                }

                sliderPosition = it

            },

            onValueChangeFinished = {

                sliderPosition?.let { onSeekFinished(it.toLong()) }

                sliderPosition = null

            },

            valueRange = 0f..totalDuration.coerceAtLeast(0L).toFloat(),

            modifier = Modifier

                .fillMaxWidth()

                .height(sliderHeight), // grosor responsivo

            colors = SliderDefaults.colors(

                thumbColor = Color(0xFF00D1A7),

                activeTrackColor = Color(0xFF00D1A7),

                inactiveTrackColor = Color(0xFF00D1A7).copy(alpha = 0.3f)

            )

        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(top = 4.dp),

            horizontalArrangement = Arrangement.SpaceBetween

        ) {


            Text(

                text = formatDuration(displayPosition.toLong()),

                fontSize = timeFontSize,

                color = Color.White

            )

            Text(

                text = formatDuration(totalDuration),

                fontSize = timeFontSize,

                color = Color.White

            )

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
    onSkipPrevious: () -> Unit,
    mainSize: Dp,
    secondarySize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        FloatingGlowButton(
            size = secondarySize,
            icon = Icons.Default.SkipPrevious,
            onClick = onSkipPrevious,
        )

        FloatingGlowButton(
            size = mainSize,
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            onClick = onTogglePlayPause,
        )

        FloatingGlowButton(
            size = secondarySize,
            icon = Icons.Default.SkipNext,
            onClick = onSkipNext,
        )
    }
}

@Composable
fun FloatingGlowButton(
    size: Dp,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Color = Color(0xFF00D1A7)
) {
    Box(
        modifier = Modifier
            .size(size)
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        // --- Glow + sombra (detrás) ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = accent.copy(alpha = 0.55f),
                    spotColor = accent.copy(alpha = 0.55f)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.35f),
                            accent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // --- Burbuja interior (gradiente) ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(8.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.55f),
                            accent.copy(alpha = 0.25f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icon (visual)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.5f)
            )
        }

        // --- Capa clicable POR ENCIMA de todo (captura el touch) ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(0.dp) // si quieres aumentar área táctil pon padding negativo/no necesario
                .clip(CircleShape)
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        )
    }
}


/**
 * Componente para activar/desactivar el control por gestos del sensor.
 */
@Composable
fun ProximitySensorControl(
    isSensorEnabled: Boolean,
    onToggleSensor: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00D1A7)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp) // menos padding vertical
    ) {
        // Escalar elementos según ancho disponible
        val scaleFactor = (maxWidth.value / 360).coerceIn(0.7f, 1f)
        val iconSize = 24.dp * scaleFactor
        val fontSize = 14.sp * scaleFactor
        val spacerWidth = 8.dp * scaleFactor

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Sensor Icon",
                    tint = neonGreen,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(spacerWidth))

                Text(
                    "Control por Gestos",
                    color = Color.White,
                    fontSize = fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = isSensorEnabled,
                onCheckedChange = onToggleSensor,
                modifier = Modifier.scale(0.60f), // Switch más pequeño
                colors = SwitchDefaults.colors(
                    checkedThumbColor = neonGreen,
                    checkedTrackColor = neonGreen.copy(alpha = 0.4f),
                    uncheckedThumbColor = Color(0xFF4A4F57),
                    uncheckedTrackColor = Color(0xFF2E343D),
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent
                )
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


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MusicPlayerPreview() {

    val fakeSong = Song(
        id = 5,
        title = "Nightfall Memories",
        artist = "Echo Dreams",
        duration = 210_000L
    )

    // Estado falso para el switch del sensor en el preview
    var sensorEnabled by remember { mutableStateOf(true) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF212121))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)

        ) {
            Spacer(modifier = Modifier.height(30.dp))
            /*AlbumArt(
                artSize = albumArtSize,
                iconSize = albumIconSize
            )*/
            Spacer(modifier = Modifier.height(10.dp))
            SongInfo(currentSong = fakeSong)

            ProximitySensorControl(
                isSensorEnabled = sensorEnabled,
                onToggleSensor = { sensorEnabled = it },
                modifier = Modifier.padding(top = 24.dp)
            )

            SongProgress(
                currentPosition = 75_000L,
                totalDuration = fakeSong.duration,
                onSeekStart = {},
                onSeekFinished = {}
            )

            PlayerControls(
                isPlaying = false,
                onTogglePlayPause = {},
                onSkipNext = {},
                onSkipPrevious = {},
                10.dp,
                10.dp
            )
        }
    }
}

//AlbumArt(
//    artSize = albumArtSize,
//    iconSize = albumIconSize
//)