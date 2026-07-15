package com.Debdoot.ciphercanvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*
import kotlin.random.Random

// Mutable 2D vector
data class Vec2(var x: Float, var y: Float)

// A single glow particle (firefly, ember, spark)
class GlowParticle(
    val pos: Vec2,
    val vel: Vec2,
    val color: Color,
    val radius: Float,
    var life: Float
)

@Composable
fun CipherCanvasArt(state: SecurityState, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 16, easing = LinearEasing)
        )
    )

    val particles = remember { mutableStateListOf<GlowParticle>() }
    val cloudOffsets = remember { List(5) { Vec2(Random.nextFloat(), Random.nextFloat()) } }

    LaunchedEffect(state) {
        particles.clear()
        val count = when (state) {
            SecurityState.SAFE -> 40
            SecurityState.SUSPICIOUS -> 30
            SecurityState.DANGER -> 60
            SecurityState.CRITICAL -> 100
        }
        val baseColor = when (state) {
            SecurityState.SAFE -> Color(0xFFFFFACD)
            SecurityState.SUSPICIOUS -> Color(0xFFFF4500)
            SecurityState.DANGER -> Color(0xFF00FFFF)
            SecurityState.CRITICAL -> Color(0xFFFF00FF)
        }
        repeat(count) {
            particles.add(
                GlowParticle(
                    pos = Vec2(Random.nextFloat(), Random.nextFloat()),
                    vel = Vec2(
                        (Random.nextFloat() - 0.5f) * 0.01f,
                        (Random.nextFloat() - 0.5f) * 0.01f + 0.002f
                    ),
                    color = baseColor.copy(alpha = Random.nextFloat() * 0.6f + 0.4f),
                    radius = Random.nextFloat() * 4f + 1f,
                    life = 1f
                )
            )
        }
    }

    LaunchedEffect(time) {
        particles.forEach { p ->
            p.pos.x += p.vel.x
            p.pos.y += p.vel.y
            p.life -= 0.002f
            if (p.life <= 0f || p.pos.x < -0.1f || p.pos.x > 1.1f || p.pos.y > 1.1f || p.pos.y < -0.1f) {
                // Respawn: mutate existing pos and vel objects, do NOT reassign the references
                p.pos.x = Random.nextFloat()
                p.pos.y = Random.nextFloat() * 0.5f
                p.life = 1f
                p.vel.x = (Random.nextFloat() - 0.5f) * 0.01f
                p.vel.y = (Random.nextFloat() - 0.5f) * 0.01f + 0.002f
            }
        }
        cloudOffsets.forEach { cloud ->
            cloud.x += 0.0003f
            if (cloud.x > 1.2f) cloud.x = -0.2f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val gradientColors = when (state) {
            SecurityState.SAFE -> listOf(Color(0xFF87CEEB), Color(0xFF4682B4))
            SecurityState.SUSPICIOUS -> listOf(Color(0xFFFFA500), Color(0xFF8B0000))
            SecurityState.DANGER -> listOf(Color(0xFF0D0221), Color(0xFF1A0033))
            SecurityState.CRITICAL -> listOf(Color(0xFF2E003E), Color(0xFF000000))
        }
        drawRect(
            brush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset(0f, 0f),
                end = Offset(0f, h)
            )
        )

        // Ground
        when (state) {
            SecurityState.SAFE -> {
                drawPath(
                    path = Path().apply {
                        moveTo(0f, h * 0.7f)
                        cubicTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.8f, w, h * 0.7f)
                        lineTo(w, h); lineTo(0f, h); close()
                    },
                    color = Color(0xFF228B22)
                )
                drawPath(
                    path = Path().apply {
                        moveTo(0f, h * 0.75f)
                        cubicTo(w * 0.2f, h * 0.7f, w * 0.5f, h * 0.85f, w, h * 0.75f)
                        lineTo(w, h); lineTo(0f, h); close()
                    },
                    color = Color(0xFF006400)
                )
            }
            SecurityState.SUSPICIOUS -> {
                drawRect(Color(0xFF8B4513), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
                repeat(10) {
                    val x = Random.nextFloat() * w
                    val y = h * 0.8f + Random.nextFloat() * h * 0.2f
                    drawLine(Color.Black, Offset(x, y), Offset(x + 20f, y + 10f), 2f)
                }
            }
            SecurityState.DANGER -> {
                drawRect(Color(0xFF0A0A0A), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
                for (i in 0..10) {
                    val x = i * w / 10f
                    drawLine(Color.Cyan.copy(alpha = 0.5f), Offset(x, h * 0.8f), Offset(x, h), 1f)
                }
            }
            SecurityState.CRITICAL -> {
                drawRect(Color.Black)
                repeat(50) {
                    val x = Random.nextFloat() * w
                    val y = Random.nextFloat() * h
                    drawRect(Color.White.copy(alpha = 0.2f), Offset(x, y), Size(5f, 5f))
                }
            }
        }

        // Sun / Eye
        when (state) {
            SecurityState.SAFE -> drawCircle(Color(0xFFFFD700), 60f, Offset(w * 0.85f, h * 0.15f))
            SecurityState.SUSPICIOUS -> {
                drawCircle(Color(0xFFFF0000), 50f, Offset(w * 0.7f, h * 0.2f))
                drawCircle(Color.Black, 15f, Offset(w * 0.7f, h * 0.2f))
            }
            SecurityState.DANGER -> drawCircle(Color.Cyan, 70f, Offset(w * 0.5f, h * 0.3f), style = Stroke(4f))
            SecurityState.CRITICAL -> {
                drawCircle(Color.Magenta, 80f, Offset(w * 0.5f, h * 0.5f), style = Stroke(3f))
                drawLine(Color.Magenta, Offset(w * 0.3f, h * 0.3f), Offset(w * 0.7f, h * 0.7f), 3f)
            }
        }

        // Clouds
        if (state == SecurityState.SAFE || state == SecurityState.SUSPICIOUS) {
            val cloudColor = if (state == SecurityState.SAFE) Color.White else Color.DarkGray
            cloudOffsets.forEachIndexed { index, offset ->
                val cx = offset.x * w
                val cy = (0.1f + index * 0.1f) * h
                drawCircle(cloudColor, 30f, Offset(cx, cy))
                drawCircle(cloudColor, 40f, Offset(cx + 30f, cy - 10f))
                drawCircle(cloudColor, 25f, Offset(cx - 20f, cy))
            }
        }

        // Particles
        particles.forEach { p ->
            val px = p.pos.x * w
            val py = p.pos.y * h
            drawCircle(color = p.color, radius = p.radius, center = Offset(px, py), alpha = p.life)
        }

        // Trees (SAFE only)
        if (state == SecurityState.SAFE) {
            drawRect(Color(0xFF8B4513), Offset(w * 0.2f, h * 0.55f), Size(10f, h * 0.2f))
            drawCircle(Color(0xFF2E8B57), 40f, Offset(w * 0.2f + 5f, h * 0.45f))
            drawRect(Color(0xFF8B4513), Offset(w * 0.7f, h * 0.5f), Size(8f, h * 0.25f))
            drawCircle(Color(0xFF2E8B57), 35f, Offset(w * 0.7f + 4f, h * 0.4f))
        }

        // Digital rain (DANGER)
        if (state == SecurityState.DANGER) {
            repeat(30) {
                val x = Random.nextFloat() * w
                val y = Random.nextFloat() * h
                drawLine(Color.Cyan.copy(alpha = 0.4f), Offset(x, y), Offset(x, y + 20f), 1f)
            }
        }
    }
}