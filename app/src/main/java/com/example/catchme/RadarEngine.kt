package com.example.catchme

import kotlin.math.pow

data class RadarBlip(
    val deviceId: String,
    val brand: String,
    val peakHeadingDegrees: Int,
    val openAirDistance: Float,
    val wallDistance: Float,
    val maxRssi: Int,
    val samplesCount: Int,
    val lastSeenMs: Long
)

class RadarEngine {

    private data class DeviceRecord(
        val blip: RadarBlip,
        val bestRssi: Int
    )

    private val activeDevices = mutableMapOf<String, DeviceRecord>()

    fun recordSignal(
        signal: BleSignal,
        headingDegrees: Int,
        totalDistance: Float
    ) {
        val rssi = if (signal.rssi == Short.MIN_VALUE.toInt() || signal.rssi == 0) -70 else signal.rssi

        val txPower = -59
        val airExp = 2.0
        val airDist = 10.0.pow((txPower - rssi) / (10.0 * airExp)).toFloat().coerceIn(0.3f, 20f)

        val wallAttenuationDb = 10.0
        val wallExp = 2.8
        val wallRssi = (rssi + wallAttenuationDb).coerceAtMost(-40.0)
        val wallDist = 10.0.pow((txPower - wallRssi) / (10.0 * wallExp)).toFloat().coerceIn(0.3f, 10f)

        val displayName = when {
            signal.deviceName.isNotBlank() &&
                signal.deviceName != "Unknown" &&
                signal.deviceName != "Nearby Device" -> signal.deviceName
            signal.brand != "Device" -> signal.brand
            else -> "Device [${signal.deviceAddress.takeLast(4)}]"
        }

        val existing = activeDevices[signal.deviceAddress]
        val isNewPeak = existing == null || rssi > existing.bestRssi

        activeDevices[signal.deviceAddress] = DeviceRecord(
            blip = RadarBlip(
                deviceId = displayName,
                brand = signal.brand,
                peakHeadingDegrees = if (isNewPeak) headingDegrees else existing!!.blip.peakHeadingDegrees,
                openAirDistance = airDist,
                wallDistance = wallDist,
                maxRssi = if (isNewPeak) rssi else existing!!.blip.maxRssi,
                samplesCount = (existing?.blip?.samplesCount ?: 0) + 1,
                lastSeenMs = System.currentTimeMillis()
            ),
            bestRssi = if (isNewPeak) rssi else existing!!.bestRssi
        )
    }

    fun computeTargets(): List<RadarBlip> {
        val now = System.currentTimeMillis()
        val timeoutMs = 25_000L

        activeDevices.entries.removeAll { now - it.value.blip.lastSeenMs > timeoutMs }

        return activeDevices.values
            .map { it.blip }
            .sortedByDescending { it.maxRssi }
            .take(8)
    }

    fun reset() {
        activeDevices.clear()
    }
}