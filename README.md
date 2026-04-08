# Edge-to-Cloud Telemetry Trustworthiness

**COMP11134 Collaborative Group Project — Group 4**

---

## 👥 Team

| Name                     | Role                                  |
| ------------------------ | ------------------------------------- |
| Craig Rutherford         | System Architecture & Validation Lead |
| Joseph Toba (Oloruntoba) | MSDK & App Implementation Lead        |
| Yassin Ginawi            | Network & Transmission Lead           |

---

## 📌 Project Overview

This project implements an end-to-end drone telemetry pipeline that streams live state data from a **DJI Mini 4 Pro** to a remote server.

The system is designed to evaluate communication **reliability, resilience, and trustworthiness** under degraded network conditions, with particular focus on **BVLOS (Beyond Visual Line of Sight)** and **SORA (Specific Operations Risk Assessment)** requirements.

---

## 🎯 Objectives

* Build a real-time telemetry pipeline from drone → cloud
* Measure system performance under unstable network conditions
* Ensure reliable data transmission with minimal loss
* Evaluate system against defined KPIs for safety-critical operations

---

## 🏗️ System Architecture

```
DJI Mini 4 Pro 
   → RC-N3 Controller 
   → Android Device (MSDK App) 
   → Network (WiFi/4G) 
   → Flask Server 
   → Data Analysis
```

---

## ⚙️ Technologies Used

* DJI MSDK v5
* Android (Kotlin)
* Python
* Flask
* Networking APIs (ConnectivityManager)

---

## 🚀 Features Implemented

* Live telemetry extraction (GPS, attitude, battery)
* Millisecond timestamping for each data packet
* Sequence numbering for packet loss detection
* HTTP transmission to remote Flask server
* Local CSV logging (`telemetry_log.csv`)
* Packet loss calculation
* Network disconnection buffering
* Automatic buffer recovery on reconnection
* RC signal quality monitoring (AirLinkKey)
* RC disconnect/reconnect event logging (`rc_events.csv`)
* Network state monitoring
* Session summary logging
* Live telemetry dashboard UI

---

## 📊 Key Performance Indicators (KPIs)

| KPI                     | Threshold                            |
| ----------------------- | ------------------------------------ |
| End-to-End Latency      | ≤ 300ms (normal), ≤ 500ms (degraded) |
| Packet Loss             | ≤ 2% (normal), ≤ 5% (degraded)       |
| Reconnect Time          | ≤ 15 seconds                         |
| Data Completeness       | ≥ 98%                                |
| Buffer Recovery Success | ≥ 99%                                |

---

## 📂 Project Structure

```
SampleCode-V5/android-sdk-v5-sample/src/main/java/dji/sampleV5/aircraft/
├── TelemetryManager.kt              # Core telemetry logic, buffering, CSV logging
├── NetworkMonitor.kt                # Network state detection
├── TelemetryDashboardActivity.kt   # Live telemetry dashboard UI
└── DJIMainActivity.kt              # SDK initialization & main activity

SampleCode-V5/android-sdk-v5-sample/src/main/res/layout/
└── activity_telemetry_dashboard.xml

server/
└── server.py                       # Flask server for telemetry reception
```

---

## 🛠️ Setup Instructions

### 🔹 Android Application

1. Clone the repository:

   ```
   git clone https://github.com/reliablejoseph30/edge-to-cloud-telemetry
   ```

2. Copy required configuration files:

   * `gradle.properties`
   * `local.properties`
   * `msdkkeystore.jks`
     *(Not included for security reasons)*

3. Open project in Android Studio:

   ```
   SampleCode-V5/android-sdk-v5-as
   ```

4. Update server IP in:

   ```
   TelemetryManager.kt
   ```

5. Build and deploy to Android device

---

### 🔹 Flask Server

```bash
cd server
pip install flask
python3 server.py
```

* Server runs on: `http://localhost:5000`
* Endpoint: `/telemetry`

---

### 📥 Retrieve Logged Data

```bash
adb pull /sdcard/Android/data/com.example.dronegroup/files/telemetry_log.csv
adb pull /sdcard/Android/data/com.example.dronegroup/files/rc_events.csv
```

---

## 🧰 Hardware Used

* DJI Mini 4 Pro Drone
* DJI RC-N3 Controller
* Android Device
* Ubuntu Development Machine

---

## 🛫 SORA / BVLOS Relevance

This project directly addresses critical **Command & Control (C2) link reliability** requirements:

* **Latency KPI** → Command response time
* **Reconnect Time** → Acceptable link loss duration
* **Data Completeness** → Navigation integrity
* **Buffer Recovery** → Prevents telemetry gaps during disconnection

---

## 🔮 Future Improvements

* Integration with ROS2 / PX4 telemetry streams
* Real-time cloud dashboard visualization
* Advanced fault-tolerant communication protocols
* AI-based anomaly detection on telemetry data

---

## 📌 Project Status

🚧 Work in Progress — Core telemetry pipeline and reliability mechanisms implemented. Ongoing testing and optimization under varied network conditions.

---

## 👤 Author Contribution (Joseph Toba)

* Implemented telemetry extraction using DJI MSDK
* Developed buffering and recovery logic
* Designed telemetry logging system (CSV + timestamps)
* Built real-time telemetry dashboard UI
* Integrated communication between mobile app and server

---
