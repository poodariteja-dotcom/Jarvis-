package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorPlasma
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    audioEnergy: Float = 0f,
    barCount: Int = 24
) {
    val transition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceAtLeast(3f)
        val step = totalWidth / barCount

        val gradient = Brush.verticalGradient(
            colors = listOf(
                ArcReactorCyan,
                ArcReactorPlasma,
                ArcReactorCyan.copy(alpha = 0.4f)
            )
        )

        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount
            val wave = (sin(normalizedIdx * 8f + phase) + 1f) / 2f
            val baseScale = if (isActive) (0.2f + (audioEnergy * 0.75f) * wave) else 0.08f
            val currentBarHeight = (maxHeight * baseScale).coerceIn(4f, maxHeight)

            val x = (i * step) + (step - barWidth) / 2f
            val y = (maxHeight - currentBarHeight) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
