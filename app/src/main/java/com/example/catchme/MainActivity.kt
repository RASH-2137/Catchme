package com.example.catchme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val radarEngine = RadarEngine()

    private val stateTargets = mutableStateOf(listOf<RadarBlip>())
    private val statePackets = mutableIntStateOf(0)
    private val stateSteps = mutableIntStateOf(0)
    private val stateDistance = mutableFloatStateOf(0f)
    private val stateHeadingDeg = mutableIntStateOf(0)
    private val stateCardinal = mutableStateOf("N")

    private val stepTracker: StepTracker by lazy {
        StepTracker(
            context = this,
            onStep = { dist, _, _ ->
                stateSteps.intValue = stepTracker.totalSteps
                stateDistance.floatValue = dist
            },
            onHeadingChanged = { degrees, cardinal ->
                stateHeadingDeg.intValue = degrees
                stateCardinal.value = cardinal
            }
        )
    }

    private val bleScanner: BleScanner by lazy {
        BleScanner(
            context = this,
            onSignalDetected = { signal ->
                radarEngine.recordSignal(
                    signal,
                    stateHeadingDeg.intValue,
                    stateDistance.floatValue
                )
                val newTargets = radarEngine.computeTargets()
                val newCount = statePackets.intValue + 1
                mainHandler.post {
                    statePackets.intValue = newCount
                    stateTargets.value = newTargets
                }
            },
            onStatusUpdate = {}
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this

        setContent {
            val targets by stateTargets
            val stepCount by stateSteps
            val distance by stateDistance
            val headingDeg by stateHeadingDeg
            val cardinal by stateCardinal

            var isScanning by remember { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.values.all { it }) {
                    isScanning = true
                    stepTracker.start()
                    bleScanner.startScan()
                } else {
                    Toast.makeText(activity, "Permissions are required to run radar", Toast.LENGTH_LONG).show()
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    RadarScreen(
                        isScanning = isScanning,
                        stepCount = stepCount,
                        distanceWalked = distance,
                        headingDegrees = headingDeg,
                        cardinalHeading = cardinal,
                        targets = targets,
                        onToggleScan = {
                            if (isScanning) {
                                isScanning = false
                                stepTracker.stop()
                                bleScanner.stopScan()
                            } else {
                                val required = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    required += Manifest.permission.BLUETOOTH_SCAN
                                    required += Manifest.permission.BLUETOOTH_CONNECT
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    required += Manifest.permission.ACTIVITY_RECOGNITION
                                }
                                val needsPermission = required.any {
                                    ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
                                }
                                if (needsPermission) {
                                    permissionLauncher.launch(required.toTypedArray())
                                } else {
                                    isScanning = true
                                    stepTracker.start()
                                    bleScanner.startScan()
                                }
                            }
                        },
                        onReset = {
                            isScanning = false
                            stepTracker.stop()
                            bleScanner.stopScan()
                            stepTracker.reset()
                            radarEngine.reset()
                            stateTargets.value = emptyList()
                            statePackets.intValue = 0
                            stateSteps.intValue = 0
                            stateDistance.floatValue = 0f
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stepTracker.stop()
        bleScanner.stopScan()
    }
}