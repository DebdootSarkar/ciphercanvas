package com.Debdoot.ciphercanvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float
)

@Composable
fun CipherCanvasArt(state: SecurityState, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing))
    )

    val particles = remember { mutableStateListOf<Particle>() }

    val backgroundColor = when (state) {
        SecurityState.SAFE -> Color(0xFF87CEEB)
        SecurityState.SUSPICIOUS -> Color(0xFFFFA500)
        SecurityState.DANGER -> Color(0xFF1A0000)
        SecurityState.CRITICAL -> Color(0xFF2E003E)
    }

    LaunchedEffect(state) {
        particles.clear()
        val count = when (state) {
            SecurityState.SAFE -> 30
            SecurityState.SUSPICIOUS -> 50
            SecurityState.DANGER -> 80
            SecurityState.CRITICAL -> 120
        }
        val baseColor = when (state) {
            SecurityState.SAFE -> Color(0xFFFFB6C1)
            SecurityState.SUSPICIOUS -> Color(0xFFD2691E)
            SecurityState.DANGER -> Color(0xFFFF0000)
            SecurityState.CRITICAL -> Color(0xFF00FFFF)
        }
        repeat(count) {
            particles.add(
                Particle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speedX = Random.nextFloat() * 0.01f - 0.005f,
                    speedY = Random.nextFloat() * 0.02f + 0.005f,
                    radius = Random.nextFloat() * 8f + 2f,
                    color = baseColor.copy(alpha = Random.nextFloat() * 0.8f + 0.2f),
                    alpha = Random.nextFloat() * 0.8f + 0.2f
                )
            )
        }
    }

    LaunchedEffect(tick) {
        particles.forEach { p ->
            p.x += p.speedX
            p.y += p.speedY
            if (p.x < -0.1f) p.x = 1.1f
            if (p.x > 1.1f) p.x = -0.1f
            if (p.y > 1.1f) p.y = -0.1f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = backgroundColor)

        particles.forEach { p ->
            val x = p.x * size.width
            val y = p.y * size.height
            drawCircle(
                color = p.color,
                radius = p.radius,
                center = Offset(x, y),
                alpha = p.alpha
            )
        }

        when (state) {
            SecurityState.SAFE -> {
                drawCircle(Color(0xFFFFD700), radius = 60f, center = Offset(size.width * 0.8f, size.height * 0.2f))
                drawCircle(Color.White, radius = 40f, center = Offset(size.width * 0.3f, size.height * 0.3f))
                drawCircle(Color.White, radius = 50f, center = Offset(size.width * 0.35f, size.height * 0.28f))
                drawCircle(Color.White, radius = 30f, center = Offset(size.width * 0.28f, size.height * 0.32f))
            }
            SecurityState.SUSPICIOUS -> {
                drawCircle(Color(0xFFFF8C00), radius = 50f, center = Offset(size.width * 0.7f, size.height * 0.25f))
                drawCircle(Color.DarkGray, radius = 45f, center = Offset(size.width * 0.4f, size.height * 0.3f))
            }
            SecurityState.DANGER -> {
                repeat(15) {
                    val startX = Random.nextFloat() * size.width
                    val startY = Random.nextFloat() * size.height
                    drawLine(Color.Red.copy(alpha = 0.5f), Offset(startX, startY),
                        Offset(startX + Random.nextFloat() * 50f, startY + Random.nextFloat() * 50f), 2f)
                }
                drawCircle(Color.White.copy(alpha = 0.3f), radius = size.width * 0.1f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f))
            }
            SecurityState.CRITICAL -> {
                drawRect(Color(0xFF2E003E))
                repeat(30) {
                    val startX = Random.nextFloat() * size.width
                    val startY = Random.nextFloat() * size.height
                    drawLine(Color.Cyan.copy(alpha = 0.7f), Offset(startX, startY),
                        Offset(startX + Random.nextFloat() * 80f - 40f, startY + Random.nextFloat() * 80f - 40f), 2f)
                }
                drawCircle(Color.White.copy(alpha = 0.5f), radius = 80f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f))
            }
        }
    }
}