 Edge-to-Cloud Telemetry Trustworthiness
**COMP11134 Collaborative Group Project — Group 4**

## Team
| Name				| Role |
|------				|------|
| Craig Rutherford 		| System Architecture & Validation Lead |
| Joseph Toba (Oloruntoba) 	| MSDK & App Implementation Lead |
| Yassin Ginawi 		| Network & Transmission Lead |

## Project Description
An end-to-end drone telemetry pipeline that streams live state data from a DJI Mini 4 Pro
to a remote server and measures performance under degraded network conditions.
The emphasis is on communication resilience and reliability in BVLOS & SORA contexts.

## System Architecture

DJI Mini 4 Pro → RC-N3 Controller → Android (MSDK App) → Network → Flask Server → Analysis

## Features Implemented
- ✅ Live telemetry extraction via DJI MSDK v5 (GPS, attitude, battery)
- ✅ Millisecond timestamping on every packet
- ✅ Sequence numbering for packet loss detection
- ✅ HTTP transmission to remote Flask server
- ✅ Local CSV file saving (telemetry_log.csv)
- ✅ Packet loss calculator
- ✅ Local buffer during network disconnection
- ✅ Buffer drain on reconnection with duration logging
- ✅ RC signal quality monitoring (AirLinkKey)
- ✅ RC disconnect/reconnect event logger (rc_events.csv)
- ✅ Network monitor (ConnectivityManager)
- ✅ Session summary logger
- ✅ Live telemetry dashboard UI

## KPIs
| KPI | Threshold |
|-----|-----------|
| End-to-End Latency | ≤ 300ms normal, ≤ 500ms degraded |
| Packet Loss | ≤ 2% normal, ≤ 5% degraded |
| Reconnect Time | ≤ 15 seconds |
| Data Completeness | ≥ 98% |
| Buffer Recovery Success | ≥ 99% |

## Project Structure

SampleCode-V5/android-sdk-v5-sample/src/main/java/dji/sampleV5/aircraft/
├── TelemetryManager.kt          # Core telemetry extraction, buffering, CSV logging
├── NetworkMonitor.kt            # Network state detection
├── TelemetryDashboardActivity.kt # Live telemetry UI dashboard
└── DJIMainActivity.kt           # Main activity, SDK registration
SampleCode-V5/android-sdk-v5-sample/src/main/res/layout/
└── activity_telemetry_dashboard.xml  # Dashboard UI layout
server/
└── server.py                    # Flask server



## Setup Instructions

### Android App
1. Clone the repository
2. Copy `gradle.properties`, `local.properties` and `msdkkeystore.jks` from the original project (not committed for security)
3. Open `SampleCode-V5/android-sdk-v5-as` in Android Studio
4. Update the server IP in `TelemetryManager.kt` to match your server
5. Build and run on Android device

### Flask Server
```bash
cd server
pip install flask
python3 server.py
```
Server runs on port 5000. Receives POST requests at `/telemetry`.

### Pull CSV Evidence
```bash
adb pull /sdcard/Android/data/com.example.dronegroup/files/telemetry_log.csv
adb pull /sdcard/Android/data/com.example.dronegroup/files/rc_events.csv
```

## Hardware
- DJI Mini 4 Pro Drone
- DJI RC-N3 Controller
- Android Device
- Ubuntu Linux Development Machine

## SORA / BVLOS Relevance
This project directly addresses SORA C2 link reliability requirements:
- Latency KPI maps to command response time requirements
- Reconnect KPI maps to acceptable loss-of-link duration
- Data completeness maps to navigation integrity assurance
- Buffer recovery ensures no telemetry gaps during link degradation
