# STM32 Bluetooth Commander – Android Dashboard

Brief Description (335 characters):
Android dashboard for STM32F103C8T6 (ARM Cortex‑M3) over HC‑05 Bluetooth SPP. Real‑time 
status LEDs (Emergency, Recording, Failure), motor ON/OFF + speed slider (0–100%), and 
action buttons. Built with Jetpack Compose, Kotlin Coroutines, Material 3. Ideal for 
wireless robotics, drone telemetry, and IoT edge control.

## Features
- **Device Selection**: Scan and connect to paired HC-05 modules.
- **Status Indicators**: Real-time LED indicators for:
  - Bluetooth Connection
  - Emergency Signal
  - Recording Status
  - System Failure
- **Motor Control**:
  - Turn Motor ON/OFF
  - Precision speed control via Slider (0-100%)
- **Action Buttons**:
  - Start/Stop Recording
  - Take Pictures

## Communication Protocol
The app communicates over a Serial Port Profile (SPP) using the standard UUID: `00001101-0000-1000-8000-00805F9B34FB`.

### Commands Sent to STM32 (Serial)
- `MOTOR_ON\n`
- `START_REC\n`
- `TAKE_PIC\n`
- `SPEED:X\n` (where X is 0-100)

### Data Received from STM32 (To update LEDs)
- `E:1` / `E:0` (Emergency)
- `R:1` / `R:0` (Recording)
- `F:1` / `F:0` (Failure)

## Tech Stack
- **Jetpack Compose**: Modern UI toolkit.
- **Kotlin Coroutines**: For asynchronous Bluetooth communication.
- **Material 3**: Latest design system.

## Setup
1. Clone the repository.
2. Open in Android Studio.
3. Build and deploy to an Android device (API 24+).
4. Pair your HC-05 module in system settings before opening the app.

---
Developed by [AdanH354](https://github.com/AdanH354)
