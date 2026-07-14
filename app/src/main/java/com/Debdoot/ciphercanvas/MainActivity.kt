package com.Debdoot.ciphercanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
        setContent {
            MaterialTheme {
                CipherCanvasScreen(monitor)
            }
        }
    }
}

@Composable
fun CipherCanvasScreen(monitor: NetworkMonitor) {
    var securityState by remember { mutableStateOf(SecurityState.DANGER) }
    var openNetworks by remember { mutableIntStateOf(0) }

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
        CipherCanvasArt(state = securityState)

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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (securityState) {
                    SecurityState.SAFE -> "☁️ Peaceful (scanner active)"
                    SecurityState.SUSPICIOUS -> "⚠️ Open network nearby"
                    SecurityState.DANGER -> "🚫 Disconnected"
                    SecurityState.CRITICAL -> "🔥 Unsecured Wi‑Fi"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Open networks: $openNetworks",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(4.dp)
            )
        }
    }
}