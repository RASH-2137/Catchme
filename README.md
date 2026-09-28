# CatchMe: Real-Time Tactical Bluetooth & BLE Radar

CatchMe is an Android application that transforms a commercial smartphone into a 360° tactical radio presence radar. By fusing dual-mode Bluetooth scanning with on-device inertial sensors and Pedestrian Dead Reckoning (PDR), the app visualizes nearby active radio nodes relative to the user's physical orientation in real time.

---

## Key Features

- **Dual-Mode Radio Sniffing**: Simultaneously intercepts Bluetooth Low Energy (BLE) advertisements and Classic Bluetooth discovery frames (`ACTION_FOUND`).
- **Heading-Up Spatial Transform**: Uses on-device `TYPE_ROTATION_VECTOR` sensor data to continuously rotate radar elements so that the user's forward heading is always mapped to the top of the display.
- **Signal-to-Distance Modeling**: Applies log-distance path loss approximation with multi-environment attenuation profiles (free-space vs. obstructed/wall models).
- **Pedestrian Dead Reckoning (PDR)**: Tracks user physical displacement using hardware step sensors (`TYPE_STEP_DETECTOR`) and directional orientation matrices.
- **Dynamic Target Filtering**: Automatically resolves manufacturer prefixes (smartwatches, smartphones, audio peripherals, IoT units) and drops stale radio traces after timeout intervals.
- **Modern Jetpack Compose Canvas**: High-performance UI rendering running hardware-accelerated animations and live target blips.

---

## System Architecture

```text
[ BLE Advertising / Classic Inquiries ]
                 │
                 ▼
          [ BleScanner ] ──(Raw Signals)──┐
                                          ▼
                                   [ RadarEngine ] ──(Target State)──┐
                                          ▲                          │
[ Device Sensors: Accelerometer/Gyro ]    │                          │
                 │                        │                          ▼
                 ▼                        │                   [ RadarScreen ]
          [ StepTracker ] ──(PDR Vectors)─┘                (Jetpack Compose UI)
```

### Components

1. **`BleScanner.kt`**: Manages hardware scan callbacks, handles Android 12/13/14+ security permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `RECEIVER_EXPORTED`), and emits uniform signal packets across hardware protocols.
2. **`RadarEngine.kt`**: Computes relative distance approximations using RSSI log-distance path loss calculations:
   $$d = 10^{\frac{TxPower - RSSI}{10 \cdot n}}$$
   It filters signal transients and maps peak heading directions.
3. **`StepTracker.kt`**: Implements step detection and rotation vector orientation parsing, converting raw quaternions to azimuth headings.
4. **`RadarScreen.kt`**: Custom Compose canvas rendering polar range rings, animated sweep beams, and relative target indicators.

---

## Tech Stack & Requirements

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Unidirectional Data Flow (State & Callbacks)
- **Min SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 36 (Android 16)
- **Hardware Dependencies**: Bluetooth Radio, Accelerometer, Magnetometer / Rotation Vector Sensor

---

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/Catchme.git
cd Catchme
```

### 2. Open in Android Studio
- Open Android Studio (Ladybug or newer recommended).
- Select **File > Open** and choose the cloned repository folder.
- Allow Gradle to sync dependencies.

### 3. Build & Run
- Connect an Android device with Developer Mode & USB Debugging enabled.
- Ensure Bluetooth and Location services are enabled on the test device.
- Click **Run ('app')** (`Shift + F10`).

---

## Permissions Handled

The application requests the following runtime permissions per Android security specifications:
- `android.permission.BLUETOOTH_SCAN`
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.ACCESS_COARSE_LOCATION`
- `android.permission.ACTIVITY_RECOGNITION`

---

## License

This project is licensed under the MIT License.
