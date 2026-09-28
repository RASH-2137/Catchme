package com.example.catchme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin

class StepTracker(
    context: Context,
    private val onStep: (distanceWalked: Float, currentX: Float, currentY: Float) -> Unit,
    private val onHeadingChanged: (degrees: Int, cardinal: String) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var currentHeadingRadians: Float = 0f
    private val strideLengthMeters: Float = 0.7f

    var totalSteps: Int = 0
        private set
    var totalDistance: Float = 0f
        private set
    var posX: Float = 0f
        private set
    var posY: Float = 0f
        private set

    fun start() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun reset() {
        totalSteps = 0
        totalDistance = 0f
        posX = 0f
        posY = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            currentHeadingRadians = orientation[0]

            val degrees = ((Math.toDegrees(currentHeadingRadians.toDouble()) + 360) % 360).toInt()
            val cardinal = resolveCardinalDirection(degrees)
            onHeadingChanged(degrees, cardinal)
        }

        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            totalSteps++
            totalDistance += strideLengthMeters

            posX += strideLengthMeters * sin(currentHeadingRadians)
            posY += strideLengthMeters * cos(currentHeadingRadians)

            onStep(totalDistance, posX, posY)
        }
    }

    private fun resolveCardinalDirection(degrees: Int): String {
        return when (degrees) {
            in 23..67 -> "NE"
            in 68..112 -> "E"
            in 113..157 -> "SE"
            in 158..202 -> "S"
            in 203..247 -> "SW"
            in 248..292 -> "W"
            in 293..337 -> "NW"
            else -> "N"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}