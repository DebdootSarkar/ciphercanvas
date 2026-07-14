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

    val targetColor = when (securityState) {
        SecurityState.SAFE -> Color(0xFF2E7D32)
        SecurityState.SUSPICIOUS -> Color(0xFFFF8F00)
        SecurityState.DANGER -> Color(0xFFC62828)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800)
    )

    val stateText = when (securityState) {
        SecurityState.SAFE -> "Network Secure"
        SecurityState.SUSPICIOUS -> "Mobile Data"
        SecurityState.DANGER -> "No Connection"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stateText,
                fontSize = 28.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (securityState) {
                    SecurityState.SAFE -> "☁️ Peaceful"
                    SecurityState.SUSPICIOUS -> "⚠️ Be Cautious"
                    SecurityState.DANGER -> "🚫 Disconnected"
                },
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}