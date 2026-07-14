package com.Debdoot.ciphercanvas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class SecurityState {
    SAFE, SUSPICIOUS, DANGER, CRITICAL
}

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val appContext = context.applicationContext
    private val mainScope = CoroutineScope(Dispatchers.Default)

    fun getSecurityStateFlow(): Flow<SecurityState> = callbackFlow {
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

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrentState()
            override fun onLost(network: Network) = emitCurrentState()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = emitCurrentState()
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        emitCurrentState()

        // Periodic Wi‑Fi scan for open networks and threat state
        val scanJob = mainScope.launch {
            while (isActive) {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val success = wifiManager.startScan()
                    if (success) {
                        delay(2000)
                        val results: List<ScanResult> = wifiManager.scanResults
                        val openNetworksCount = results.count {
                            !it.capabilities.contains("EAP") &&
                            !it.capabilities.contains("PSK") &&
                            !it.capabilities.contains("WEP")
                        }
                        val wifiInfo = wifiManager.connectionInfo
                        val currentOpen = wifiInfo != null &&
                                wifiInfo.ssid != null &&
                                wifiInfo.networkId != -1 &&
                                (wifiInfo.supplicantState.name == "COMPLETED") &&
                                !isWifiSecure(wifiInfo)
                        val totalOpen = openNetworksCount + if (currentOpen) 1 else 0
                        val threatState = when {
                            totalOpen == 0 -> SecurityState.SAFE
                            totalOpen in 1..2 -> SecurityState.SUSPICIOUS
                            totalOpen in 3..5 -> SecurityState.DANGER
                            else -> SecurityState.CRITICAL
                        }
                        trySend(threatState)
                    }
                }
                delay(10_000)
            }
        }

        awaitClose {
            scanJob.cancel()
            mainScope.cancel()
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    fun getOpenNetworkCountFlow(): Flow<Int> = callbackFlow {
        val scanJob = mainScope.launch {
            while (isActive) {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val success = wifiManager.startScan()
                    if (success) {
                        delay(2000)
                        val results = wifiManager.scanResults
                        val openCount = results.count {
                            !it.capabilities.contains("EAP") &&
                            !it.capabilities.contains("PSK") &&
                            !it.capabilities.contains("WEP")
                        }
                        trySend(openCount)
                    }
                }
                delay(10_000)
            }
        }
        awaitClose { scanJob.cancel() }
    }

    private fun isWifiSecure(info: android.net.wifi.WifiInfo): Boolean {
        val capabilities = wifiManager.scanResults.firstOrNull { it.SSID == info.ssid }?.capabilities ?: ""
        return capabilities.contains("WPA") || capabilities.contains("WEP") || capabilities.contains("PSK")
    }
}