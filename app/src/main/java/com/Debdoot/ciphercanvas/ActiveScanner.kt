package com.Debdoot.ciphercanvas

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress

enum class ScanState {
    IDLE,
    SCANNING,
    COMPLETE,
    ERROR
}

class ActiveScanner {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState

    private val _discoveredHosts = MutableStateFlow<List<String>>(emptyList())
    val discoveredHosts: StateFlow<List<String>> = _discoveredHosts

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress

    fun startScan() {
        if (_scanState.value != ScanState.IDLE) return
        _scanState.value = ScanState.SCANNING
        _discoveredHosts.value = emptyList()
        _scanProgress.value = 0f
        scope.launch {
            try {
                val localIp = InetAddress.getLocalHost().hostAddress
                val prefix = localIp?.substringBeforeLast(".") ?: "192.168.1"
                val hosts = mutableListOf<String>()
                for (i in 1..254) {
                    val ip = "$prefix.$i"
                    val reachable = InetAddress.getByName(ip).isReachable(200)
                    if (reachable) {
                        hosts.add(ip)
                        _discoveredHosts.value = hosts.toList()
                    }
                    _scanProgress.value = i.toFloat() / 254f
                }
                _scanState.value = ScanState.COMPLETE
            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
            }
        }
    }

    fun reset() {
        _scanState.value = ScanState.IDLE
        _discoveredHosts.value = emptyList()
        _scanProgress.value = 0f
    }
}