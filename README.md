# 👟 Smart Sole: IoT-Enabled Foot Pressure Monitoring System

## Overview
Website Link: https://sites.google.com/uw.edu/smartsole/lab-notebook/week-9-10

**Smart Sole** is a wearable IoT system designed to monitor **foot pressure distribution** in real time using embedded sensors, Bluetooth Low Energy (BLE), and an Android mobile application. This system aims to provide insights into posture, gait analysis, and injury prevention, and is compatible with various shoe types for users ranging from athletes to physical therapy patients.

This project was developed over 10 weeks as part of a senior capstone course in a team of four.

---

## 🔧 Features

- **Custom Insole Hardware**
  - 6 Force-Sensitive Resistors (FSRs) embedded in key foot pressure points
  - IMU sensor (accelerometer + gyroscope)
  - Temperature sensor
  - ESP32 BLE-enabled microcontroller

- **Custom PCB + 3D-Printed Enclosure**
  - Compact, wearable circuit board layout
  - Shoe-friendly printed housing

- **Android Mobile App (Kotlin + Jetpack Compose)**
  - Real-time pressure visualization on a foot diagram
  - Bluetooth connection and auto-reconnect with Adafruit microcontroller
  - Live sensor feed: FSR, IMU, temperature, and system status
  - Packet history log with timestamp and data integrity check

- **Machine Learning Module**
  - Activity recognition based on foot pressure signatures
  - Trained to detect walking, standing, and shifting weight
---

## 📡 How It Works

1. **Sensor data** is collected on the ESP32 microcontroller.
2. Data is structured into packets and transmitted over **Bluetooth Low Energy (BLE)** using a Nordic UART service.
3. The Android app receives and parses the data, displaying pressure zones, motion, and system feedback in real time.
4. Packet history and live system status are visualized for user insight and debugging.

---

## 🛠 Technologies Used

- **Hardware**: ESP32, FSRs, MPU6050 IMU, temperature sensor, custom PCB
- **Embedded Firmware**: Arduino C/C++
- **Mobile App**: Kotlin, Jetpack Compose, BLE GATT API, Android Studio
- **Visualization & UI**: Figma (design), Jetpack Compose (frontend)
- **Machine Learning**: Python (training and classification models)

---


## 📈 Key Takeaways

- Learned full-stack development from design to physical prototype
- Gained experience with Bluetooth communication and real-time embedded systems
- Developed strong cross-functional collaboration skills
- Practiced rapid iteration and testing under a tight 10-week timeline

---



