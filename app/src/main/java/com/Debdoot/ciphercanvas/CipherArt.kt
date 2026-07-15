package com.Debdoot.ciphercanvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlin.math.*
import kotlin.random.Random

data class GlowParticle(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var radius: Float,
    var color: Color,
    var life: Float
)

@Composable
fun CipherCanvasArt(
    state: SecurityState,
    scanActive: Boolean = false,
    scanProgress: Float = 0f,
    discoveredHosts: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16, easing = LinearEasing))
    )

    val particles = remember { mutableStateListOf<GlowParticle>() }

    // Background color palette per state
    val bgColors = when (state) {
        SecurityState.SAFE -> listOf(Color(0xFF1a237e), Color(0xFF4a148c))   // deep blue/purple peaceful
        SecurityState.SUSPICIOUS -> listOf(Color(0xFFb71c1c), Color(0xFFffab00)) // red/orange
        SecurityState.DANGER -> listOf(Color(0xFF212121), Color(0xFFb71c1c)) // dark/red
        SecurityState.CRITICAL -> listOf(Color(0xFF000000), Color(0xFF4a148c)) // black/purple
    }

    // Particle color per state
    val particleColor = when (state) {
        SecurityState.SAFE -> Color(0xFF64ffda)      // cyan
        SecurityState.SUSPICIOUS -> Color(0xFFffab00) // amber
        SecurityState.DANGER -> Color(0xFFf44336)    // red
        SecurityState.CRITICAL -> Color(0xFFea80fc)  // magenta
    }

    // Reset particles when state changes
    LaunchedEffect(state) {
        particles.clear()
        val count = when (state) {
            SecurityState.SAFE -> 50
            SecurityState.SUSPICIOUS -> 40
            SecurityState.DANGER -> 70
            SecurityState.CRITICAL -> 100
        }
        repeat(count) {
            particles.add(
                GlowParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speedX = (Random.nextFloat() - 0.5f) * 0.005f,
                    speedY = (Random.nextFloat() - 0.5f) * 0.005f - 0.002f,
                    radius = Random.nextFloat() * 5f + 2f,
                    color = particleColor,
                    life = Random.nextFloat() * 0.5f + 0.5f
                )
            )
        }
    }

    // Animate particles each frame
    LaunchedEffect(time) {
        particles.forEach { p ->
            p.x += p.speedX
            p.y += p.speedY
            p.life -= 0.003f
            if (p.life <= 0f || p.x < -0.1f || p.x > 1.1f || p.y > 1.1f || p.y < -0.1f) {
                p.x = Random.nextFloat()
                p.y = Random.nextFloat() * 0.8f
                p.life = 1f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Flowing gradient background
        drawRect(
            brush = Brush.linearGradient(
                colors = bgColors,
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Glowing orb (sun/eye)
        val sunRadius = when (state) {
            SecurityState.SAFE -> 70f
            SecurityState.SUSPICIOUS -> 55f
            SecurityState.DANGER -> 40f
            SecurityState.CRITICAL -> 30f
        }
        val sunColor = when (state) {
            SecurityState.SAFE -> Color(0xFFffd54f)
            SecurityState.SUSPICIOUS -> Color(0xFFff7043)
            SecurityState.DANGER -> Color(0xFFef5350)
            SecurityState.CRITICAL -> Color(0xFFab47bc)
        }
        drawCircle(sunColor, sunRadius, Offset(w * 0.85f, h * 0.15f), alpha = 0.8f)
        // outer glow
        drawCircle(sunColor.copy(alpha = 0.3f), sunRadius * 1.5f, Offset(w * 0.85f, h * 0.15f))

        // Particles
        particles.forEach { p ->
            val px = p.x * w
            val py = p.y * h
            drawCircle(p.color, p.radius, Offset(px, py), alpha = p.life)
        }

        // ---- Boss battle overlay (unchanged) ----
        if (scanActive) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Red.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w / 2, h / 2),
                    radius = w * 0.8f
                )
            )
            drawArc(
                color = Color.Cyan,
                startAngle = -90f,
                sweepAngle = 360f * scanProgress,
                useCenter = false,
                topLeft = Offset(w / 2 - 60f, h / 2 - 60f),
                size = Size(120f, 120f),
                style = Stroke(width = 6f)
            )
            drawCircle(Color.Black, 40f, Offset(w / 2, h / 2))
            drawCircle(Color.White, 20f, Offset(w / 2, h / 2))
            drawCircle(Color.Red, 8f, Offset(w / 2, h / 2))

            discoveredHosts.forEachIndexed { index, ip ->
                val angle = (index * 137.5f) % 360f
                val radius = 150f + scanProgress * 50f
                val x = w / 2 + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
                val y = h / 2 + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
                drawCircle(Color.Red, 15f, Offset(x, y))
                drawCircle(Color.Yellow, 5f, Offset(x, y))
                drawLine(Color.Red.copy(alpha = 0.3f), Offset(w / 2, h / 2), Offset(x, y), 1f)
            }
        }
    }
}