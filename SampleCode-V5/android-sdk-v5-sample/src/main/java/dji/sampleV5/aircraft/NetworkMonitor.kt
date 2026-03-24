package dji.sampleV5.aircraft

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

object NetworkMonitor {

    private const val TAG = "NETWORK"

    fun start(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request,
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available")
                    TelemetryManager.onNetworkRestored()
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    TelemetryManager.onNetworkLost()
                }
            })

        Log.d(TAG, "NetworkMonitor started")
    }
}