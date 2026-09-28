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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val radarEngine = RadarEngine()

    // All state lives at Activity level so any thread can safely update via mainHandler.post
    val stateTargets      = mutableStateOf(listOf<RadarBlip>())
    val statePackets      = mutableIntStateOf(0)
    val stateLastDevice   = mutableStateOf("---")
    val stateLastRssi     = mutableIntStateOf(0)
    val stateScanStatus   = mutableStateOf("IDLE")
    val stateSteps        = mutableIntStateOf(0)
    val stateDistance     = mutableFloatStateOf(0f)
    val stateUserX        = mutableFloatStateOf(0f)
    val stateUserY        = mutableFloatStateOf(0f)
    val stateHeadingDeg   = mutableIntStateOf(0)
    val stateCardinal     = mutableStateOf("N")

    // Created ONCE here — never recreated inside a composable
    private val stepTracker: StepTracker by lazy {
        StepTracker(
            context = this,
            onStep = { dist, x, y ->
                // Step sensor fires on main thread already
                stateSteps.intValue    = stepTracker.totalSteps
                stateDistance.floatValue = dist
                stateUserX.floatValue  = x
                stateUserY.floatValue  = y
            },
            onHeadingChanged = { degrees, cardinal ->
                stateHeadingDeg.intValue = degrees
                stateCardinal.value      = cardinal
            }
        )
    }

    private val bleScanner: BleScanner by lazy {
        BleScanner(
            context = this,
            onSignalDetected = { signal ->
                // Runs on BT hardware thread — post to main
                radarEngine.recordSignal(
                    signal,
                    stateHeadingDeg.intValue,   // Current compass heading
                    stateDistance.floatValue
                )
                val newTargets = radarEngine.computeTargets()
                val newCount   = statePackets.intValue + 1
                mainHandler.post {
                    statePackets.intValue    = newCount
                    stateLastDevice.value    = signal.deviceName.take(18)
                    stateLastRssi.intValue   = signal.rssi
                    stateTargets.value       = newTargets
                }
            },
            onStatusUpdate = { status ->
                mainHandler.post { stateScanStatus.value = status }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this

        setContent {
            // Observe all activity-level state as composable state
            val targets     by stateTargets
            val packets     by statePackets
            val lastDevice  by stateLastDevice
            val lastRssi    by stateLastRssi
            val scanStatus  by stateScanStatus
            val stepCount   by stateSteps
            val distance    by stateDistance
            val userX       by stateUserX
            val userY       by stateUserY
            val headingDeg  by stateHeadingDeg
            val cardinal    by stateCardinal

            var isScanning        by remember { mutableStateOf(false) }
            var corridorWidth     by remember { mutableFloatStateOf(3.0f) }
            val walkPath          = remember { mutableStateListOf(Offset(0f, 0f)) }

            // Sync walk path when position changes
            LaunchedEffect(userX, userY) {
                if (userX != 0f || userY != 0f) {
                    walkPath.add(Offset(userX, userY))
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.values.all { it }) {
                    isScanning = true
                    stepTracker.start()
                    bleScanner.startScan()
                } else {
                    Toast.makeText(activity, "All permissions required!", Toast.LENGTH_LONG).show()
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    RadarScreen(
                        isScanning        = isScanning,
                        stepCount         = stepCount,
                        distanceWalked    = distance,
                        userX             = userX,
                        userY             = userY,
                        headingDegrees    = headingDeg,
                        cardinalHeading   = cardinal,
                        corridorWidthMeters = corridorWidth,
                        onCorridorWidthChanged = { corridorWidth = it },
                        walkPath          = walkPath,
                        targets           = targets,
                        onToggleScan      = {
                            if (isScanning) {
                                isScanning = false
                                stepTracker.stop()
                                bleScanner.stopScan()
                                stateScanStatus.value = "STOPPED"
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
                                    ContextCompat.checkSelfPermission(activity, it) !=
                                            PackageManager.PERMISSION_GRANTED
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
                            walkPath.clear()
                            walkPath.add(Offset(0f, 0f))
                            stateTargets.value    = emptyList()
                            statePackets.intValue = 0
                            stateLastDevice.value = "---"
                            stateSteps.intValue   = 0
                            stateDistance.floatValue = 0f
                            stateUserX.floatValue = 0f
                            stateUserY.floatValue = 0f
                            stateScanStatus.value = "IDLE"
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