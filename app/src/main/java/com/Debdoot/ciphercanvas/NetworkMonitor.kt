package com.Debdoot.ciphercanvas

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NetworkMonitor(private val context: Context) {

    fun getConnectionCountFlow(): Flow<Int> = flow {
        while (true) {
            val count = countActiveConnections()
            emit(count)
            delay(60_000L)
        }
    }

    private fun countActiveConnections(): Int {
        var count = 0
        try {
            val process = Runtime.getRuntime().exec("cat /proc/net/tcp")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() // skip header
            var line: String? = reader.readLine()
            while (line != null) {
                count++
                line = reader.readLine()
            }
            reader.close()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}