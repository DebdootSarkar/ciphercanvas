package com.Debdoot.ciphercanvas

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
                val hosts = mutableListOf<String>()
                val simulatedHosts = listOf("192.168.1.1", "192.168.1.5", "192.168.1.10")
                for (i in 1..50) {
                    delay(150)
                    _scanProgress.value = i / 50f
                    if (i == 20 && hosts.size < simulatedHosts.size) {
                        hosts.add(simulatedHosts[0])
                        _discoveredHosts.value = hosts.toList()
                    }
                    if (i == 35 && hosts.size < simulatedHosts.size) {
                        hosts.add(simulatedHosts[1])
                        _discoveredHosts.value = hosts.toList()
                    }
                    if (i == 45 && hosts.size < simulatedHosts.size) {
                        hosts.add(simulatedHosts[2])
                        _discoveredHosts.value = hosts.toList()
                    }
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