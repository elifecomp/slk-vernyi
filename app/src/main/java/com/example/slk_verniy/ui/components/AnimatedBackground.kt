package com.example.slk_verniy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Bubble(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val speed: Float,
    val amplitude: Float,
    val frequency: Float,
    val phase: Float,
    val color: Color,
    val alpha: Float
)

@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AnimatedBackgroundTransition")

    val bgProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgProgress"
    )

    val bubblesProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubblesProgress"
    )

    // Rich blue and cyan color palette
    val deepNavy = Color(0xFF030712)
    val deepBlue = Color(0xFF0B192C)
    val royalBlue = Color(0xFF1E3A8A)
    val cyanAccent = Color(0xFF38BDF8)
    val lightCyan = Color(0xFF7DD3FC)

    // Pre-generate static list of bubbles so they remain consistent across recompositions
    val bubbles = remember {
        val random = Random(42) // Fixed seed for aesthetic consistency
        val list = mutableListOf<Bubble>()
        val colors = listOf(cyanAccent, lightCyan, royalBlue, Color(0xFF0284C7), Color(0xFFE0F2FE))
        repeat(14) {
            list.add(
                Bubble(
                    xRatio = random.nextFloat(),
                    yRatio = random.nextFloat(),
                    radius = random.nextFloat() * 35f + 15f, // 15f to 50f radius
                    speed = random.nextFloat() * 0.8f + 0.4f,
                    amplitude = random.nextFloat() * 60f + 20f,
                    frequency = random.nextFloat() * 2f + 1f,
                    phase = random.nextFloat() * 6.28f,
                    color = colors[random.nextInt(colors.size)],
                    alpha = random.nextFloat() * 0.4f + 0.2f
                )
            )
        }
        list
    }

    val gradientCenter = Offset(
        x = 500f + sin(bgProgress * 6.28f) * 300f,
        y = 500f + cos(bgProgress * 6.28f) * 300f
    )

    val brush = Brush.radialGradient(
        colors = listOf(
            cyanAccent.copy(alpha = 0.25f),
            royalBlue.copy(alpha = 0.35f),
            deepBlue.copy(alpha = 0.8f),
            deepNavy
        ),
        center = gradientCenter,
        radius = 1600f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(deepNavy)
            .background(brush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            bubbles.forEach { bubble ->
                // Animate upward floating with wrap around
                val totalTravel = height + bubble.radius * 3f
                val currentY = height + bubble.radius - ((bubble.yRatio * height + bubblesProgress * totalTravel * bubble.speed) % totalTravel)
                
                // Horizontal swaying motion using sine wave
                val sway = sin(bubblesProgress * 6.28f * bubble.frequency + bubble.phase) * bubble.amplitude
                val currentX = (bubble.xRatio * width + sway).coerceIn(bubble.radius, width - bubble.radius)

                // Draw glowing aura / soft background circle
                drawCircle(
                    color = bubble.color.copy(alpha = bubble.alpha * 0.3f),
                    radius = bubble.radius * 1.4f,
                    center = Offset(currentX, currentY)
                )

                // Draw main bubble body with radial gradient for 3D bubble effect
                val bubbleBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = bubble.alpha * 0.9f),
                        bubble.color.copy(alpha = bubble.alpha),
                        bubble.color.copy(alpha = bubble.alpha * 0.2f)
                    ),
                    center = Offset(currentX - bubble.radius * 0.3f, currentY - bubble.radius * 0.3f),
                    radius = bubble.radius * 1.2f
                )

                drawCircle(
                    brush = bubbleBrush,
                    radius = bubble.radius,
                    center = Offset(currentX, currentY)
                )

                // Draw subtle glossy highlight reflection
                drawCircle(
                    color = Color.White.copy(alpha = bubble.alpha * 0.7f),
                    radius = bubble.radius * 0.25f,
                    center = Offset(currentX - bubble.radius * 0.35f, currentY - bubble.radius * 0.35f)
                )

                // Draw delicate thin outline
                drawCircle(
                    color = Color.White.copy(alpha = bubble.alpha * 0.5f),
                    radius = bubble.radius,
                    center = Offset(currentX, currentY),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        content()
    }
}
