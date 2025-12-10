package com.example.integradorasensorproximidad.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clase auxiliar que encapsula toda la lógica para detectar gestos
 * con el sensor de proximidad.
 */
class ProximitySensorHelper(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val isPlayingProvider: () -> Boolean,
    private val onTogglePlayPause: () -> Unit,
    private val onSkipNext: () -> Unit,
    private val onSkipPrevious: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private sealed class ProximityState {
        object Far : ProximityState()
        data class Near(val startTime: Long, val longPressTriggered: Boolean, val wasPlaying: Boolean) : ProximityState()
    }
    private var sensorState: ProximityState = ProximityState.Far
    private var waveCount = 0
    private var waveHandlerJob: Job? = null
    private val LONG_PRESS_THRESHOLD_MS = 500L
    private val WAVE_RESET_TIMEOUT_MS = 700L

    val isSensorAvailable: Boolean
        get() = proximitySensor != null

    fun startListening() {
        if (!isSensorAvailable) return
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopListening() {
        if (!isSensorAvailable) return
        sensorManager.unregisterListener(this)
        waveHandlerJob?.cancel()
        sensorState = ProximityState.Far
        waveCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return

        val isCurrentlyNear = event.values[0] < (proximitySensor?.maximumRange ?: 5.0f)
        val previousState = sensorState

        if (isCurrentlyNear && previousState is ProximityState.Far) {
            // Estado: Lejos -> Cerca
            val now = System.currentTimeMillis()
            sensorState = ProximityState.Near(startTime = now, longPressTriggered = false, wasPlaying = isPlayingProvider())
            coroutineScope.launch {
                delay(LONG_PRESS_THRESHOLD_MS)
                val stateAfterDelay = sensorState
                if (stateAfterDelay is ProximityState.Near && stateAfterDelay.startTime == now) {
                    // Es un gesto largo
                    if (stateAfterDelay.wasPlaying) {
                        onTogglePlayPause() // Pausa la música
                    }
                    sensorState = stateAfterDelay.copy(longPressTriggered = true)
                }
            }
        } else if (!isCurrentlyNear && previousState is ProximityState.Near) {
            // Estado: Cerca -> Lejos
            sensorState = ProximityState.Far

            if (previousState.longPressTriggered) {
                // Si se activó el gesto largo, reanudamos la música
                if (previousState.wasPlaying) {
                    onTogglePlayPause() // Reanuda la música
                }
            } else {
                // Si fue un gesto corto (un "wave")
                waveCount++
                waveHandlerJob?.cancel()
                waveHandlerJob = coroutineScope.launch {
                    delay(WAVE_RESET_TIMEOUT_MS)
                    if (waveCount == 1) {
                        onSkipNext()
                    } else if (waveCount >= 2) {
                        onSkipPrevious()
                    }
                    waveCount = 0
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario para este caso de uso.
    }
}
