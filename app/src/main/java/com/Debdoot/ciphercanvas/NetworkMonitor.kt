package com.Debdoot.ciphercanvas

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class SecurityState {
    SAFE,        // Wi-Fi or Ethernet
    SUSPICIOUS,  // Mobile data only
    DANGER       // No network
}

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getSecurityStateFlow(): Flow<SecurityState> = callbackFlow {
        // Helper function defined at the outer scope so it's accessible
        fun emitCurrentState() {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val state = when {
                caps == null -> SecurityState.DANGER
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> SecurityState.SAFE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> SecurityState.SAFE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> SecurityState.SUSPICIOUS
                else -> SecurityState.DANGER
            }
            trySend(state)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrentState()
            override fun onLost(network: Network) = emitCurrentState()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = emitCurrentState()
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        emitCurrentState() // initial emission

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}