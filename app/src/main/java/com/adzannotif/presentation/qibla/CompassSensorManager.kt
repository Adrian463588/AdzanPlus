package com.adzannotif.presentation.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompassSensorManager @Inject constructor(
    private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun getHeadingFlow(): Flow<Float> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        var lastAccelerometer = FloatArray(3)
        var lastMagnetometer = FloatArray(3)
        var lastAccelerometerSet = false
        var lastMagnetometerSet = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthInRadians = orientationAngles[0]
                    val azimuthInDegrees = ((Math.toDegrees(azimuthInRadians.toDouble()) + 360) % 360).toFloat()
                    trySend(azimuthInDegrees)
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                        lastAccelerometerSet = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                        lastMagnetometerSet = true
                    }
                    if (lastAccelerometerSet && lastMagnetometerSet) {
                        SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val azimuthInRadians = orientationAngles[0]
                        val azimuthInDegrees = ((Math.toDegrees(azimuthInRadians.toDouble()) + 360) % 360).toFloat()
                        trySend(azimuthInDegrees)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
