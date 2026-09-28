# ⚡ CatchMe // Tactical Bluetooth Radar

An innovative Android tactical navigation and radio sniffing radar built with **Jetpack Compose** and **Kotlin**. 

CatchMe turns your smartphone into a real-time Bluetooth detection HUD that tracks nearby devices (phones, smartwatches, earbuds, smart TVs), estimates their distance through walls, and dynamically rotates the radar canvas using your device's hardware compass — like a video game mini-map.

---

## 🌟 Key Features

- **📡 Dual-Mode Radio Sniffer:**
  - **Bluetooth Low Energy (BLE):** Scans smartwatches, fitness trackers, and beacons with low-latency callbacks.
  - **Classic Bluetooth (BR/EDR):** Discovers broadcast-enabled smartphones, wireless headphones, and TVs.
- **🧭 "Heading-Up" 360° Tactical Radar:**
  - Directly binds to Android's `TYPE_ROTATION_VECTOR` hardware sensor.
  - As you turn your body, the entire radar rotates smoothly so the direction you are facing is always **UP**.
  - Turn towards any target blip to face it directly in the real world.
- **🚶 Pedestrian Dead Reckoning (PDR):**
  - Integrated with `TYPE_STEP_DETECTOR` to track real steps taken and walking distance in meters.
- **🧱 Wall Attenuation & Path-Loss Distance Modeling:**
  - Implements logarithmic radio frequency path-loss equations.
  - Dynamically calculates two distances for every device: **Open Air Distance** ($n = 2.0$) and **Through-Wall Distance** ($n = 2.8$ with drywall attenuation factor).
- **🎯 Smart Target Inspector:**
  - Expandable HUD drawer displaying active targets sorted by signal strength (RSSI).
  - Automatically times out and prunes silent devices after 25 seconds.
  - Custom brand & model recognition (boAt, XTEND, Fire-Boltt, Apple, Samsung, etc.).
- **🎨 Modern Tactical Interface:**
  - Built 100% in Jetpack Compose with custom hardware-accelerated Canvas rendering.
  - Custom player avatar at center with live degree and cardinal badge.

---

## 📸 Screenshots

| Tactical Radar HUD | Target Inspector Drawer |
|:---:|:---:|
| *(Add your screenshot here)* | *(Add your screenshot here)* |

---

## 🛠️ Architecture & Tech Stack

- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (Material3)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 / 36 (Android 14 / Android 16 ready)
- **Hardware Sensors:**
  - `BluetoothAdapter` & `BluetoothLeScanner`
  - `Sensor.TYPE_ROTATION_VECTOR`
  - `Sensor.TYPE_STEP_DETECTOR`

### Project Structure
```text
com.example.catchme
├── MainActivity.kt      // Application entry point, system coordination, permission handling
├── BleScanner.kt        // Dual-mode radio scanner (BLE + Classic discovery)
├── RadarEngine.kt       // RF path-loss calculation, peak heading estimation & target tracking
├── StepTracker.kt       // Pedestrian Dead Reckoning (PDR) step counting & compass calculations
└── RadarScreen.kt       // Custom Jetpack Compose Canvas radar, sweep animation & HUD drawer
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- Android device running Android 8.0+ (Must support Bluetooth LE and Gyroscope/Magnetometer)

### Clone & Run
```bash
git clone https://github.com/<YOUR_USERNAME>/CatchMe.git
cd CatchMe
```
1. Open the project in **Android Studio**.
2. Connect your phone via USB (or install the pre-built APK).
3. Grant **Location**, **Nearby Devices**, and **Physical Activity** permissions when prompted.
4. Tap **START RADAR** and walk around!

---

## 👨‍💻 Author
- **Rahul**
- Built with passion as an Android Systems & Sensor Integration project.
