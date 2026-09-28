package com.example.catchme

import kotlin.math.pow

data class RadarBlip(
    val deviceId: String,      // Best available name
    val brand: String,
    val peakHeadingDegrees: Int,  // Compass heading when signal was strongest
    val openAirDistance: Float,
    val wallDistance: Float,
    val maxRssi: Int,
    val samplesCount: Int,
    val lastSeenMs: Long           // For stale-device timeout
)

class RadarEngine {

    private data class DeviceRecord(
        val blip: RadarBlip,
        val bestRssi: Int
    )

    private val activeDevices = mutableMapOf<String, DeviceRecord>()

    /**
     * Call this every time a BLE/Classic packet is received.
     * headingDegrees = current phone compass heading when this packet arrived.
     */
    fun recordSignal(
        signal: BleSignal,
        headingDegrees: Int,
        totalDistance: Float
    ) {
        val rssi = if (signal.rssi == Short.MIN_VALUE.toInt() || signal.rssi == 0) -70 else signal.rssi

        // ── Distance math ─────────────────────────────────────────────────
        // Reference TX power at 1m (dBm). Typical BLE beacon: -59
        val txPower      = -59
        val airExp       = 2.0          // Free-space path loss exponent
        val airDist      = 10.0.pow((txPower - rssi) / (10.0 * airExp)).toFloat()
            .coerceIn(0.3f, 20f)        // 0.3m – 20m, unrestricted

        // Wall model: assume 30cm drywall absorbs ~10 dB
        val wallRssi     = (rssi + 10.0).coerceAtMost(-40.0)
        val wallExp      = 2.8
        val wallDist     = 10.0.pow((txPower - wallRssi) / (10.0 * wallExp)).toFloat()
            .coerceIn(0.3f, 10f)

        // ── Best name logic ─────────────────────────────────────────────
        // Priority: real device name > brand > short MAC
        val displayName = when {
            signal.deviceName.isNotBlank()
                    && signal.deviceName != "Unknown"
                    && signal.deviceName != "Nearby Device" -> signal.deviceName
            signal.brand != "Device" && signal.brand != "BLE Node" -> signal.brand
            else -> "${signal.brand} [${signal.deviceAddress.takeLast(4)}]"
        }

        val existing = activeDevices[signal.deviceAddress]
        val isNewPeak = existing == null || rssi > existing.bestRssi

        activeDevices[signal.deviceAddress] = DeviceRecord(
            blip = RadarBlip(
                deviceId           = displayName,
                brand              = signal.brand,
                // Only update heading when we get a stronger (peak) signal
                peakHeadingDegrees = if (isNewPeak) headingDegrees
                                     else existing!!.blip.peakHeadingDegrees,
                openAirDistance    = airDist,
                wallDistance       = wallDist,
                maxRssi            = if (isNewPeak) rssi else existing!!.blip.maxRssi,
                samplesCount       = (existing?.blip?.samplesCount ?: 0) + 1,
                lastSeenMs         = System.currentTimeMillis()
            ),
            bestRssi = if (isNewPeak) rssi else existing!!.bestRssi
        )
    }

    /**
     * Returns active targets, sorted strongest-first, stale ones removed.
     * Max 8 so the UI stays clean.
     */
    fun computeTargets(): List<RadarBlip> {
        val now    = System.currentTimeMillis()
        val cutoff = 25_000L   // Remove device if not seen for 25 seconds

        // Purge stale entries in-place
        activeDevices.entries.removeAll { now - it.value.blip.lastSeenMs > cutoff }

        return activeDevices.values
            .map { it.blip }
            .sortedByDescending { it.maxRssi }   // Strongest signal first
            .take(8)
    }

    fun reset() {
        activeDevices.clear()
    }
}