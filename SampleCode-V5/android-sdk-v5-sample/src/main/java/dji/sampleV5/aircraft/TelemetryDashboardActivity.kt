package dji.sampleV5.aircraft

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.databinding.ActivityTelemetryDashboardBinding

class TelemetryDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTelemetryDashboardBinding
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 500L

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelemetryDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateUI() {
        val packet = TelemetryManager.buildPacket()
        binding.tvBattery.text = "🔋 Battery: ${packet.battery}%"
        binding.tvSignal.text = "📶 Signal Quality: ${packet.signalQuality}%"
        binding.tvGPS.text = "📍 GPS: Lat ${String.format("%.6f", packet.lat)}, Lon ${String.format("%.6f", packet.lon)}, Alt ${String.format("%.1f", packet.alt)}m"
        binding.tvAttitude.text = "✈️ Attitude: Roll ${String.format("%.1f", packet.roll)}° Pitch ${String.format("%.1f", packet.pitch)}° Yaw ${String.format("%.1f", packet.yaw)}°"
        binding.tvPackets.text = "📦 Packets Sent: ${packet.seq}"
        binding.tvPacketLoss.text = "❌ Packet Loss: ${"%.2f".format(TelemetryManager.getPacketLossPercent())}%"
        binding.tvRCStatus.text = "🎮 RC Connected: ${if (packet.rcConnected) "YES ✅" else "NO ❌"}"
        binding.tvBufferSize.text = "💾 Buffer Size: ${TelemetryManager.getBufferSize()} packets"
    }
}