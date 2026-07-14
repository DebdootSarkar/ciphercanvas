package com.yourname.ciphercanvas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val monitor = NetworkMonitor(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ActiveConnectionsScreen(monitor)
                }
            }
        }
    }
}

@Composable
fun ActiveConnectionsScreen(monitor: NetworkMonitor) {
    var connectionCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        monitor.getConnectionCountFlow().collectLatest { count ->
            connectionCount = count
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Active TCP Connections: $connectionCount",
            fontSize = 24.sp
        )
    }
}