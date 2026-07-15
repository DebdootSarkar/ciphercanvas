package com.Debdoot.ciphercanvas

import android.graphics.Bitmap
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

// ── Particle for floating petals / embers ──────────────────────────
data class GlowParticle(
    var x: Float, var y: Float,
    var speedX: Float, var speedY: Float,
    var radius: Float, var color: Color, var life: Float
)

@Composable
fun CipherCanvasArt(
    state: SecurityState,
    scanActive: Boolean = false,
    scanProgress: Float = 0f,
    discoveredHosts: List<String> = emptyList(),
    aiBitmap: Bitmap? = null,   // ← AI plug‑in point (unused for now)
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(16, easing = LinearEasing)))

    val particles = remember { mutableStateListOf<GlowParticle>() }

    // ── Cloud offsets (for parallax) ───────────────────────────────
    val clouds = remember {
        List(6) {
            CloudLayer(
                offset = Random.nextFloat(),
                speed = Random.nextFloat() * 0.02f + 0.01f,
                yBase = Random.nextFloat() * 0.3f + 0.1f,
                scale = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    // ── Respawn particles on state change ─────────────────────────
    LaunchedEffect(state) {
        particles.clear()
        val count = when (state) {
            SecurityState.SAFE -> 50
            SecurityState.SUSPICIOUS -> 40
            SecurityState.DANGER -> 70
            SecurityState.CRITICAL -> 100
        }
        val baseColor = when (state) {
            SecurityState.SAFE -> Color(0xFFFFB6C1)   // sakura pink
            SecurityState.SUSPICIOUS -> Color(0xFFFF7043) // orange ember
            SecurityState.DANGER -> Color(0xFFEF5350)    // red spark
            SecurityState.CRITICAL -> Color(0xFFEA80FC)  // magenta static
        }
        repeat(count) {
            particles.add(GlowParticle(
                x = Random.nextFloat(), y = Random.nextFloat(),
                speedX = (Random.nextFloat() - 0.5f) * 0.003f,
                speedY = (Random.nextFloat() - 0.5f) * 0.003f - 0.001f,
                radius = Random.nextFloat() * 4f + 1.5f,
                color = baseColor,
                life = Random.nextFloat() * 0.5f + 0.5f
            ))
        }
    }

    // ── Animate particles each frame ─────────────────────────────
    LaunchedEffect(time) {
        particles.forEach { p ->
            p.x += p.speedX; p.y += p.speedY
            p.life -= 0.003f
            if (p.life <= 0f || p.x < -0.1f || p.x > 1.1f || p.y > 1.1f || p.y < -0.1f) {
                p.x = Random.nextFloat(); p.y = Random.nextFloat() * 0.7f
                p.life = 1f
            }
        }
        clouds.forEach { it.offset = (it.offset + it.speed) % 1.1f }
    }

    // ── The Canvas ───────────────────────────────────────────────
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width; val h = size.height

        // 1) Background gradient (or AI bitmap if available)
        if (aiBitmap != null) {
            // drawImage not available in Compose Canvas directly, use inline Bitmap
            // For now we keep procedural; we'll add the drawImage later.
        } else {
            val colors = when (state) {
                SecurityState.SAFE -> listOf(Color(0xFF1a237e), Color(0xFF4a148c))
                SecurityState.SUSPICIOUS -> listOf(Color(0xFFb71c1c), Color(0xFFffab00))
                SecurityState.DANGER -> listOf(Color(0xFF0D0221), Color(0xFF1A0033))
                SecurityState.CRITICAL -> listOf(Color(0xFF000000), Color(0xFF2E003E))
            }
            drawRect(Brush.linearGradient(colors, Offset(0f, 0f), Offset(w, h)))
        }

        // 2) Stars (only in DANGER / CRITICAL)
        if (state == SecurityState.DANGER || state == SecurityState.CRITICAL) {
            val starCount = if (state == SecurityState.DANGER) 30 else 60
            repeat(starCount) {
                val sx = Random.nextFloat() * w
                val sy = Random.nextFloat() * h * 0.7f
                val alpha = Random.nextFloat() * 0.5f + 0.3f
                drawCircle(Color.White.copy(alpha = alpha), 1.5f, Offset(sx, sy))
            }
        }

        // 3) Hills (only in SAFE / SUSPICIOUS)
        if (state == SecurityState.SAFE || state == SecurityState.SUSPICIOUS) {
            val hillColor = if (state == SecurityState.SAFE)
                listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
            else
                listOf(Color(0xFF5D4037), Color(0xFF3E2723))

            drawPath(
                Path().apply {
                    moveTo(0f, h * 0.75f)
                    cubicTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.8f, w, h * 0.72f)
                    lineTo(w, h); lineTo(0f, h); close()
                },
                Brush.linearGradient(hillColor, Offset(0f, h * 0.7f), Offset(0f, h))
            )
        }

        // 4) Aurora waves (energy ribbons) – intensity rises with danger
        val auroraAlpha = when (state) {
            SecurityState.SAFE -> 0.0f
            SecurityState.SUSPICIOUS -> 0.15f
            SecurityState.DANGER -> 0.35f
            SecurityState.CRITICAL -> 0.55f
        }
        if (auroraAlpha > 0f) {
            for (i in 0..3) {
                val y = h * (0.2f + i * 0.15f) + sin(time * 2 * PI + i) * 15f
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.Cyan.copy(alpha = auroraAlpha), Color.Transparent),
                        startX = 0f, endX = w
                    ),
                    start = Offset(0f, y.toFloat()), end = Offset(w, y.toFloat()),
                    strokeWidth = 4f
                )
            }
        }

        // 5) Parallax clouds
        clouds.forEach { cloud ->
            val cx = cloud.offset * w
            val cy = cloud.yBase * h
            drawCircle(Color.White.copy(alpha = 0.6f), 30f * cloud.scale, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.6f), 40f * cloud.scale, Offset(cx + 40f, cy - 10f))
            drawCircle(Color.White.copy(alpha = 0.6f), 25f * cloud.scale, Offset(cx - 25f, cy))
        }

        // 6) Sun / Eye (shrinks as danger rises)
        val sunRadius = when (state) {
            SecurityState.SAFE -> 70f
            SecurityState.SUSPICIOUS -> 55f
            SecurityState.DANGER -> 35f
            SecurityState.CRITICAL -> 20f
        }
        val sunColor = when (state) {
            SecurityState.SAFE -> Color(0xFFffd54f)
            SecurityState.SUSPICIOUS -> Color(0xFFff7043)
            SecurityState.DANGER -> Color(0xFFef5350)
            SecurityState.CRITICAL -> Color(0xFFab47bc)
        }
        val sunPos = Offset(w * 0.85f, h * 0.15f)
        // outer glow
        drawCircle(sunColor.copy(alpha = 0.25f), sunRadius * 2f, sunPos)
        drawCircle(sunColor, sunRadius, sunPos)

        // 7) Particles
        particles.forEach { p ->
            val px = p.x * w; val py = p.y * h
            drawCircle(p.color, p.radius, Offset(px, py), alpha = p.life)
        }

        // 8) Boss battle overlay (exactly as before)
        if (scanActive) {
            drawRect(Brush.radialGradient(
                colors = listOf(Color.Red.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w / 2, h / 2), radius = w * 0.8f
            ))
            drawArc(Color.Cyan, -90f, 360f * scanProgress, false,
                topLeft = Offset(w / 2 - 60f, h / 2 - 60f),
                size = Size(120f, 120f), style = Stroke(6f))
            drawCircle(Color.Black, 40f, Offset(w / 2, h / 2))
            drawCircle(Color.White, 20f, Offset(w / 2, h / 2))
            drawCircle(Color.Red, 8f, Offset(w / 2, h / 2))

            discoveredHosts.forEachIndexed { i, _ ->
                val angle = (i * 137.5f) % 360f
                val r = 150f + scanProgress * 50f
                val x = w / 2 + r * cos(Math.toRadians(angle.toDouble())).toFloat()
                val y = h / 2 + r * sin(Math.toRadians(angle.toDouble())).toFloat()
                drawCircle(Color.Red, 15f, Offset(x, y))
                drawCircle(Color.Yellow, 5f, Offset(x, y))
                drawLine(Color.Red.copy(alpha = 0.3f), Offset(w / 2, h / 2), Offset(x, y), 1f)
            }
        }
    }
}

// ── Helper data class for cloud layers ────────────────────────────
private data class CloudLayer(
    var offset: Float,
    val speed: Float,
    val yBase: Float,
    val scale: Float
)