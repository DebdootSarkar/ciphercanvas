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

    Box(modifier = Modifier.fillMaxSize()) {
        // Art engine with scanner overlay
        CipherCanvasArt(
            state = securityState,
            scanActive = scanState == ScanState.SCANNING,
            scanProgress = scanProgress,
            discoveredHosts = discoveredHosts
        )

        // Bottom info text
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (securityState) {
                    SecurityState.SAFE -> "Network Secure"
                    SecurityState.SUSPICIOUS -> "Mobile Data"
                    SecurityState.DANGER -> "No Connection"
                    SecurityState.CRITICAL -> "⚠️ Open Network Alert"
                },
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Scanner status
            when (scanState) {
                ScanState.SCANNING -> {
                    Text(
                        text = "⚡ Scanning: ${(scanProgress * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = Color.Cyan,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp)
                    )
                }
                ScanState.COMPLETE -> {
                    Text(
                        text = "✅ Scan complete: ${discoveredHosts.size} hosts found",
                        fontSize = 14.sp,
                        color = Color.Cyan,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp)
                    )
                }
                ScanState.ERROR -> {
                    Text(
                        text = "❌ Scan error",
                        fontSize = 14.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp)
                    )
                }
                else -> {}
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open networks: $openNetworks",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(4.dp)
            )
        }

        // Floating Action Button for scan
        FloatingActionButton(
            onClick = {
                if (scanState == ScanState.IDLE || scanState == ScanState.COMPLETE) {
                    scanner.startScan()
                } else {
                    scanner.reset()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Start ARP Scan",
                tint = Color.White
            )
        }
    }
}