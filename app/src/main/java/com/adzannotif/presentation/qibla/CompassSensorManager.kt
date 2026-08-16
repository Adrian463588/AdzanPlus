package com.adzannotif.presentation.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class CompassSensorData(
    val azimuthDegrees: Float,
    val accuracy: Int,
    val isSensorAvailable: Boolean,
)

@Singleton
class CompassSensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun getHeadingFlow(): Flow<CompassSensorData> = callbackFlow {
        if (sensorManager == null) {
            trySend(CompassSensorData(0f, SensorManager.SENSOR_STATUS_UNRELIABLE, false))
            close()
            return@callbackFlow
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val hasSensors = rotationSensor != null || (accelerometer != null && magnetometer != null)

        if (!hasSensors) {
            trySend(CompassSensorData(0f, SensorManager.SENSOR_STATUS_UNRELIABLE, false))
            awaitClose { }
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        var lastAccelerometer = FloatArray(3)
        var lastMagnetometer = FloatArray(3)
        var lastAccelerometerSet = false
        var lastMagnetometerSet = false
        var smoothedAzimuth = 0f
        var isFirstReading = true

        val alpha = 0.25f // Low-pass filter smoothing coefficient

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var currentAzimuth = 0f

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthInRadians = orientationAngles[0]
                    currentAzimuth = ((Math.toDegrees(azimuthInRadians.toDouble()) + 360) % 360).toFloat()
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        for (i in 0..2) {
                            lastAccelerometer[i] = lastAccelerometer[i] + alpha * (event.values[i] - lastAccelerometer[i])
                        }
                        lastAccelerometerSet = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        for (i in 0..2) {
                            lastMagnetometer[i] = lastMagnetometer[i] + alpha * (event.values[i] - lastMagnetometer[i])
                        }
                        lastMagnetometerSet = true
                    }

                    if (lastAccelerometerSet && lastMagnetometerSet) {
                        val success = SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
                        if (success) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val azimuthInRadians = orientationAngles[0]
                            currentAzimuth = ((Math.toDegrees(azimuthInRadians.toDouble()) + 360) % 360).toFloat()
                        } else {
                            return
                        }
                    } else {
                        return
                    }
                }

                // Smooth circular azimuth transition
                if (isFirstReading) {
                    smoothedAzimuth = currentAzimuth
                    isFirstReading = false
                } else {
                    val diff = ((currentAzimuth - smoothedAzimuth + 180f) % 360f + 360f) % 360f - 180f
                    smoothedAzimuth = (smoothedAzimuth + alpha * diff + 360f) % 360f
                }

                trySend(
                    CompassSensorData(
                        azimuthDegrees = smoothedAzimuth,
                        accuracy = event.accuracy,
                        isSensorAvailable = true
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
