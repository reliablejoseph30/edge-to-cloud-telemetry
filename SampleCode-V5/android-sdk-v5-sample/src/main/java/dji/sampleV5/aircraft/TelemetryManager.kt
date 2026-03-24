package dji.sampleV5.aircraft

import android.util.Log
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.v5.manager.KeyManager
import dji.sdk.keyvalue.key.KeyTools
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import org.json.JSONObject

object TelemetryManager {

    private val sequenceCounter = AtomicInteger(0)
    private const val TAG = "TELEMETRY"

    // ── Telemetry values ──────────────────────────────────────────────────
    private var lat = 0.0
    private var lon = 0.0
    private var alt = 0.0
    private var roll = 0.0
    private var pitch = 0.0
    private var yaw = 0.0
    private var battery = 0

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
        val battery: Int
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
            }.toString()
        }
    }

    // ── Telemetry listeners ───────────────────────────────────────────────
    fun startListening() {
        Log.d(TAG, "TelemetryManager starting...")

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

        KeyManager.getInstance().listen(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent), this
        ) { oldValue: Int?, newValue: Int? ->
            if (newValue != null) {
                battery = newValue
                Log.d(TAG, "Battery → $battery%")
            }
        }
    }

    // ── Packet handling ───────────────────────────────────────────────────
    private fun handleNewPacket() {
        val packet = buildPacket()
        if (isNetworkAvailable) {
            // Network is up — send directly
            sendPacket(packet)
        } else {
            // Network is down — cache locally
            buffer.add(packet)
            Log.d(TAG, "BUFFERED seq=${packet.seq} | Buffer size: ${buffer.size}")
        }
    }

    // ── Send packet (Yassin will replace this with real HTTP/MQTT call) ───
    private fun sendPacket(packet: TelemetryPacket) {
        Log.d(TAG, "SENT seq=${packet.seq} | ${packet.toJson()}")
        // TODO: Yassin — replace this log with actual HTTP POST or MQTT publish
    }

    // ── Network state management ──────────────────────────────────────────
    fun onNetworkLost() {
        isNetworkAvailable = false
        disconnectTimestamp = System.currentTimeMillis()
        Log.d(TAG, "NETWORK LOST — buffering started at $disconnectTimestamp")
    }

    fun onNetworkRestored() {
        val reconnectTime = System.currentTimeMillis()
        val duration = (reconnectTime - disconnectTimestamp) / 1000.0
        isNetworkAvailable = true
        Log.d(TAG, "NETWORK RESTORED — reconnect duration: ${duration}s | Buffer size: ${buffer.size}")
        drainBuffer()
    }

    // ── Buffer drain ──────────────────────────────────────────────────────
    private fun drainBuffer() {
        var count = 0
        while (buffer.isNotEmpty()) {
            val packet = buffer.poll()
            if (packet != null) {
                sendPacket(packet)
                count++
            }
        }
        Log.d(TAG, "BUFFER DRAINED — $count packets replayed")
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    fun buildPacket(): TelemetryPacket {
        return TelemetryPacket(
            seq = sequenceCounter.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            lat = lat, lon = lon, alt = alt,
            roll = roll, pitch = pitch, yaw = yaw,
            battery = battery
        )
    }

    fun getBufferSize(): Int = buffer.size

    fun stopListening() {
        KeyManager.getInstance().cancelListen(this)
        Log.d(TAG, "TelemetryManager stopped.")
    }
}