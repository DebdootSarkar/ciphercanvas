package com.Debdoot.ciphercanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val LOCATION_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }

        val monitor = NetworkMonitor(applicationContext)
        val activeScanner = ActiveScanner()
        setContent {
            MaterialTheme {
                CipherCanvasScreen(monitor, activeScanner, this)
            }
        }
    }
}

@Composable
fun CipherCanvasScreen(monitor: NetworkMonitor, scanner: ActiveScanner, activity: ComponentActivity) {
    var securityState by remember { mutableStateOf(SecurityState.DANGER) }
    var openNetworks by remember { mutableIntStateOf(0) }
    val scanState by scanner.scanState.collectAsState()
    val discoveredHosts by scanner.discoveredHosts.collectAsState()
    val scanProgress by scanner.scanProgress.collectAsState()

    LaunchedEffect(Unit) {
        monitor.getSecurityStateFlow().collectLatest { state ->
            securityState = state
        }
    }
    LaunchedEffect(Unit) {
        monitor.getOpenNetworkCountFlow().collectLatest { count ->
            openNetworks = count
        }
    }

    // Reset scanner if network is lost (avoids stale banner)
    LaunchedEffect(securityState) {
        if (securityState == SecurityState.DANGER) {
            scanner.reset()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CipherCanvasArt(
            state = securityState,
            scanActive = scanState == ScanState.SCANNING,
            scanProgress = scanProgress,
            discoveredHosts = discoveredHosts
        )

        // Top banner for scan status (only show if not DANGER)
        if ((scanState == ScanState.SCANNING || scanState == ScanState.COMPLETE) && securityState != SecurityState.DANGER) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (scanState) {
                        ScanState.SCANNING -> "⚡ ARP Sweep: ${(scanProgress * 100).toInt()}%"
                        ScanState.COMPLETE -> "✅ Scan complete – ${discoveredHosts.size} hosts found"
                        else -> ""
                    },
                    color = Color.Cyan,
                    fontSize = 14.sp
                )
            }
        }

        // Bottom status bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (securityState) {
                    SecurityState.SAFE -> "Network Secure"
                    SecurityState.SUSPICIOUS -> "Mobile Data"
                    SecurityState.DANGER -> "No Connection"
                    SecurityState.CRITICAL -> "⚠️ Open Network Alert"
                },
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = "Open networks: $openNetworks",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Scan button (top-right)
        IconButton(
            onClick = {
                if (securityState != SecurityState.DANGER &&
                    (scanState == ScanState.IDLE || scanState == ScanState.COMPLETE)
                ) {
                    scanner.startScan()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color(0x80000000), shape = MaterialTheme.shapes.small)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "ARP Scan",
                tint = if (scanState == ScanState.SCANNING) Color.Cyan else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}