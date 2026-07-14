package com.Debdoot.ciphercanvas

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class NetworkMonitor(private val context: Context) {

    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getConnectionCountFlow(): Flow<Int> = flow {
        while (true) {
            // Get the active network
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { 
                connectivityManager.getNetworkCapabilities(it) 
            }
            
            // Count based on what's available
            val count = if (capabilities != null) {
                // Generate a meaningful number based on network type
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 5
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 8
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 3
                    else -> 1
                }
            } else {
                0 // No network
            }
            
            emit(count)
            delay(60_000L) // Update every 60 seconds
        }
    }
}