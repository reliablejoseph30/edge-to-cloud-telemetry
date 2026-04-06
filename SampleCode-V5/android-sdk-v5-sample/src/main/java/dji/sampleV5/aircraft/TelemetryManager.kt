package dji.sampleV5.aircraft

import android.util.Log
import dji.sdk.keyvalue.key.AirLinkKey
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.v5.manager.KeyManager
import dji.sdk.keyvalue.key.KeyTools
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import org.json.JSONObject
import java.io.File
import android.content.Context

object TelemetryManager {

    private val sequenceCounter = AtomicInteger(0)
    private const val TAG = "TELEMETRY"

    private var appContext: Context? = null

    // ── Telemetry values ──────────────────────────────────────────────────
    private var lat = 0.0
    private var lon = 0.0
    private var alt = 0.0
    private var roll = 0.0
    private var pitch = 0.0
    private var yaw = 0.0
    private var battery = 0
    private var signalQuality = 0
    private var rcConnected = false

    private var lastSeq = -1
    private var totalPackets = 0
    private var lostPackets = 0
    private var reconnectCount = 0

    // ── Buffer ────────────────────────────────────────────────────────────
    private val buffer = ConcurrentLinkedQueue<TelemetryPacket>()
    private var isNetworkAvailable = true
    private var disconnectTimestamp = 0L

    data class TelemetryPacket(
        val seq: Int,
        val timestamp: Long,
        val lat: Double,
        val lon: Double,
        val alt: Double,
        val roll: Double,
        val pitch: Double,
        val yaw: Double,
        val battery: Int,
        val signalQuality: Int,
        val rcConnected: Boolean
    ) {
        fun toJson(): String {
            return JSONObject().apply {
                put("seq", seq)
                put("timestamp", timestamp)
                put("lat", lat)
                put("lon", lon)
                put("alt", alt)
                put("roll", roll)
                put("pitch", pitch)
                put("yaw", yaw)
                put("battery", battery)
                put("signal_quality", signalQuality)
                put("rc_connected", rcConnected)
            }.toString()
        }
    }

    // ── Telemetry listeners ───────────────────────────────────────────────
    fun startListening() {
        Log.d(TAG, "TelemetryManager starting...")

        // GPS + Altitude
        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D), this
        ) { oldValue: LocationCoordinate3D?, newValue: LocationCoordinate3D? ->
            if (newValue != null) {
                lat = newValue.latitude
                lon = newValue.longitude
                alt = newValue.altitude
                Log.d(TAG, "GPS → Lat: $lat, Lon: $lon, Alt: $alt")
                handleNewPacket()
            }
        }

        // Attitude
        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude), this
        ) { oldValue: Attitude?, newValue: Attitude? ->
            if (newValue != null) {
                roll = newValue.roll
                pitch = newValue.pitch
                yaw = newValue.yaw
                Log.d(TAG, "Attitude → Roll: $roll, Pitch: $pitch, Yaw: $yaw")
            }
        }

        // Battery state
        KeyManager.getInstance().listen(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent), this
        ) { oldValue: Int?, newValue: Int? ->
            if (newValue != null) {
                battery = newValue
                Log.d(TAG, "Battery → $battery%")
            }
        }

        // Drone Controller's Connection State and feedback
        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyConnection), this
        ) { oldValue: Boolean?, newValue: Boolean? ->
            if (newValue != null) {
                val wasConnected = rcConnected
                rcConnected = newValue
                if (wasConnected && !newValue) {
                    val disconnectTime = System.currentTimeMillis()
                    Log.d(TAG, "RC LINK LOST — Signal was: $signalQuality% at $disconnectTime")
                    Log.d(TAG, "RC LINK LOST — Packets buffered: ${buffer.size}")
                    saveRCEventToCSV("DISCONNECTED", disconnectTime)
                } else if (!wasConnected && newValue) {
                    val reconnectTime = System.currentTimeMillis()
                    Log.d(TAG, "RC LINK RESTORED — Signal now: $signalQuality% at $reconnectTime")
                    saveRCEventToCSV("RECONNECTED", reconnectTime)
                }
                Log.d(TAG, "RC-Controller Connected: $rcConnected")
            }
        }

        // Controller Signal Quality (uplink quality %)
        KeyManager.getInstance().listen(
            KeyTools.createKey(AirLinkKey.KeyUpLinkQuality), this
        ) { oldValue: Int?, newValue: Int? ->
            if (newValue != null) {
                signalQuality = newValue
                Log.d(TAG, "Signal Quality: $signalQuality%")
            }
        }
    }

    // ── Packet handling with csv save ───────────────────────────────────────────────────
    private fun handleNewPacket() {
        val packet = buildPacket()
        saveToCSV(packet) // Always save locally regardless of network>> Added
        calculatePacketLoss(packet.seq) // added to calculate packet loss during network failure
        if (isNetworkAvailable) {
            sendPacket(packet)
        } else {
            buffer.add(packet)
            Log.d(TAG, "BUFFERED seq=${packet.seq} | Buffer size: ${buffer.size}")
        }
    }

    // ── Send packet function to Yassin's server (HTTP) ───────────────────────────────────────────────────────
    private fun sendPacket(packet: TelemetryPacket) {
        Log.d(TAG, "SENT seq=${packet.seq} | Signal: ${packet.signalQuality}% | ${packet.toJson()}")
        Thread {
            try {
                val url = java.net.URL("http://10.66.73.250:5000/telemetry")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.outputStream.write(packet.toJson().toByteArray())
                val responseCode = conn.responseCode
                Log.d(TAG, "HTTP response: $responseCode seq=${packet.seq}")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Send failed seq=${packet.seq}: ${e.message}")
                buffer.add(packet)
            }
        }.start()
    }

    // ── Network state management to monitor when network fails and reconnects ──────────────────────────────────────────
    fun onNetworkLost() {
        isNetworkAvailable = false
        disconnectTimestamp = System.currentTimeMillis()
        Log.d(TAG, "NETWORK LOST — buffering started at $disconnectTimestamp")
    }

    fun onNetworkRestored() {
        val reconnectTime = System.currentTimeMillis()
        isNetworkAvailable = true
        if (disconnectTimestamp > 0) {
            val duration = (reconnectTime - disconnectTimestamp) / 1000.0
            Log.d(TAG, "NETWORK RESTORED — reconnect duration: ${duration}s | Buffer size: ${buffer.size}")
        } else {
            Log.d(TAG, "NETWORK RESTORED — initial connection | Buffer size: ${buffer.size}")
        }
        reconnectCount++ //count reconnects >>> Added
        Log.d(TAG, "RECONNECT #$reconnectCount")
        disconnectTimestamp = 0L
        Thread { drainBuffer() }.start()
    }

    // ── Buffer drain ──────────────────────────────────────────────────────
    private fun drainBuffer() {
        var count = 0
        try {
            while (buffer.isNotEmpty()) {
                val packet = buffer.poll()
                if (packet != null) {
                    sendPacket(packet)
                    count++
                }
            }
            Log.d(TAG, "BUFFER DRAINED — $count packets replayed")
        } catch (e: Exception) {
            Log.e(TAG, "Buffer drain error: ${e.message}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    fun buildPacket(): TelemetryPacket {
        return TelemetryPacket(
            seq = sequenceCounter.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            lat = lat, lon = lon, alt = alt,
            roll = roll, pitch = pitch, yaw = yaw,
            battery = battery,
            signalQuality = signalQuality,
            rcConnected = rcConnected
        )
    }

    fun getBufferSize(): Int = buffer.size
    // ── Packet Loss Calculator ────────────────────────────────────────────
    private fun calculatePacketLoss(seq: Int) {
        totalPackets++
        if (lastSeq >= 0 && seq != lastSeq + 1) {
            val lost = seq - lastSeq - 1
            lostPackets += lost
            Log.d(TAG, "PACKET LOSS DETECTED — missing $lost packets | Total loss: ${getPacketLossPercent()}%")
        }
        lastSeq = seq
    }

    fun getPacketLossPercent(): Double {
        return if (totalPackets == 0) 0.0
        else (lostPackets.toDouble() / (totalPackets + lostPackets)) * 100
    }

    // ── Session Summary ───────────────────────────────────────────────────
    fun printSessionSummary() {
        Log.d(TAG, "═══════════════════════════════")
        Log.d(TAG, "SESSION SUMMARY")
        Log.d(TAG, "Total packets sent: ${sequenceCounter.get()}")
        Log.d(TAG, "Packet loss: ${"%.2f".format(getPacketLossPercent())}%")
        Log.d(TAG, "Reconnect count: $reconnectCount")
        Log.d(TAG, "Buffer size at end: ${buffer.size}")
        Log.d(TAG, "═══════════════════════════════")

    }

    fun stopListening() {
        KeyManager.getInstance().cancelListen(this)
        Log.d(TAG, "TelemetryManager stopped.")
    }
    // ── Adding CSV file & Context ─────────────────────────────────────────────────────
    fun init(context: Context) {
        appContext = context.applicationContext
        // Write CSV header if file doesn't exist
        val file = File(context.getExternalFilesDir(null), "telemetry_log.csv")
        if (!file.exists()) {
            file.writeText("seq,timestamp,lat,lon,alt,roll,pitch,yaw,battery,signal_quality,rc_connected\n")
            Log.d(TAG, "CSV file created: ${file.absolutePath}")
        }
    }

    private fun saveToCSV(packet: TelemetryPacket) {
        try {
            val file = File(appContext?.getExternalFilesDir(null), "telemetry_log.csv")
            file.appendText("${packet.seq},${packet.timestamp},${packet.lat},${packet.lon},${packet.alt},${packet.roll},${packet.pitch},${packet.yaw},${packet.battery},${packet.signalQuality},${packet.rcConnected}\n")
        } catch (e: Exception) {
            Log.e(TAG, "CSV save error: ${e.message}")
        }
    }
    // ── RC Event Logger ───────────────────────────────────────────────────
    private fun saveRCEventToCSV(event: String, timestamp: Long) {
        try {
            val file = File(appContext?.getExternalFilesDir(null), "rc_events.csv")
            if (!file.exists()) {
                file.writeText("event,timestamp,signal_quality,battery,lat,lon\n")
            }
            file.appendText("$event,$timestamp,$signalQuality,$battery,$lat,$lon\n")
            Log.d(TAG, "RC EVENT SAVED: $event at $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "RC event save error: ${e.message}")
        }
    }
}