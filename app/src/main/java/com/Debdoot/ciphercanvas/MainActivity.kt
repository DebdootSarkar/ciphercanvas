package com.Debdoot.ciphercanvas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    LaunchedEffect(Unit) {
        monitor.getSecurityStateFlow().collectLatest { state ->
            securityState = state
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The animated art background
        CipherCanvasArt(state = securityState)

        // Overlay text (semi-transparent so art shows through)
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
                },
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            )
        }
    }
}