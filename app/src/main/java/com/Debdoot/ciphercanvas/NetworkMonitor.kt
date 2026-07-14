package com.Debdoot.ciphercanvas

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.*

enum class SecurityState {
    SAFE,        // Secure Wi‑Fi or Ethernet
    SUSPICIOUS,  // Mobile data or unknown
    DANGER,      // No connection
    CRITICAL     // Open/unsecure Wi‑Fi or many open networks nearby
}

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val mainScope = CoroutineScope(Dispatchers.Default)

    fun getSecurityStateFlow(): Flow<SecurityState> = callbackFlow {
        // ---- Helper: compute state from current connectivity & Wi‑Fi data ----
        fun emitCurrentState() {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val state = when {
                caps == null -> SecurityState.DANGER   // no network at all
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // Wi‑Fi is connected – check its security
                    val wifiInfo = wifiManager.connectionInfo
                    val isOpen = wifiInfo != null && wifiInfo.supplicantState.name == "COMPLETED" &&
                            (wifiInfo.ssid != null) && (wifiInfo.networkId != -1)
                    // We'll determine open vs secure later; for now, treat Wi‑Fi as SAFE
                    SecurityState.SAFE
                }
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

        // Emit initial state
        emitCurrentState()

        // ---- Periodic Wi‑Fi scan to detect open networks ----
        // We start a coroutine that scans every 10 seconds and updates the state
        val scanJob = mainScope.launch {
            while (isActive) {
                // Only scan if we have permission and Wi‑Fi is enabled
                if (ContextCompat.checkSelfPermission(context.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val success = wifiManager.startScan()
                    if (success) {
                        // Wait a moment for scan results to be available
                        delay(2000)
                        val results: List<ScanResult> = wifiManager.scanResults
                        val openNetworksCount = results.count { it.capabilities.contains("EAP") == false && it.capabilities.contains("PSK") == false && it.capabilities.contains("WEP") == false }
                        // Also check if current Wi‑Fi is open
                        val wifiInfo = wifiManager.connectionInfo
                        val currentNetworkOpen = wifiInfo != null && wifiInfo.ssid != null &&
                                (wifiInfo.supplicantState.name == "COMPLETED") &&
                                (wifiInfo.networkId != -1) &&
                                (wifiInfo.hiddenSSID == false) &&
                                !isWifiSecure(wifiInfo)
                        
                        // Determine threat state based on open networks
                        val totalOpen = openNetworksCount + if (currentNetworkOpen) 1 else 0
                        val newState = when {
                            totalOpen == 0 -> SecurityState.SAFE
                            totalOpen in 1..2 -> SecurityState.SUSPICIOUS
                            totalOpen in 3..5 -> SecurityState.DANGER
                            else -> SecurityState.CRITICAL
                        }
                        trySend(newState)
                    }
                }
                delay(10_000) // scan every 10 seconds
            }
        }

        awaitClose {
            scanJob.cancel()
            mainScope.cancel()
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun isWifiSecure(wifiInfo: android.net.wifi.WifiInfo): Boolean {
        val capabilities = wifiInfo.ssid?.let { wifiManager.connectionInfo?.let { info ->
            // This is a workaround: we can't easily get capabilities for current network,
            // but we can check if the network is configured with a password.
            // For simplicity, assume it's secure if it's WPA/WPA2 (we'll rely on the scan result of itself later).
            true // Placeholder – in a real implementation we'd use WifiConfiguration.
        } } ?: true
        return capabilities == true
    }
}