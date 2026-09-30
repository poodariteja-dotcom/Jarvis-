package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.ArcReactorPlasma
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorOrb(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    audioEnergy: Float = 0f,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_anim")

    // Slow rotation for outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Reverse faster rotation for inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Breathing pulse for idle glow
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val activeScale = when {
        isListening -> 1.0f + (audioEnergy * 0.45f)
        isSpeaking -> pulseGlow * 1.08f
        else -> pulseGlow
    }

    val primaryGlow = when {
        isListening -> ArcReactorCyan
        isSpeaking -> ArcReactorPlasma
        else -> ArcReactorCyan.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = ArcReactorCyan),
                onClick = onClick
            )
            .testTag("arc_reactor_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.85f
            val currentRadius = baseRadius * activeScale.coerceIn(0.8f, 1.4f)

            // 1. Outermost Ambient Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.35f),
                        primaryGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.25f
                ),
                radius = currentRadius * 1.25f,
                center = center
            )

            // 2. Rotating Outer Segmented Ring
            rotate(outerRotation, pivot = center) {
                val segments = 8
                val sweep = 30f
                val gap = (360f / segments)
                for (i in 0 until segments) {
                    val startAngle = i * gap
                    drawArc(
                        color = ArcReactorCyan.copy(alpha = 0.75f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - currentRadius, center.y - currentRadius),
                        size = androidx.compose.ui.geometry.Size(currentRadius * 2, currentRadius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Middle Counter-Rotating Ring
            rotate(innerRotation, pivot = center) {
                val middleRadius = currentRadius * 0.72f
                val segments = 4
                val sweep = 65f
                val gap = (360f / segments)
                for (i in 0 until segments) {
                    val startAngle = i * gap
                    drawArc(
                        color = ArcReactorGold.copy(alpha = 0.65f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                        size = androidx.compose.ui.geometry.Size(middleRadius * 2, middleRadius * 2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Node points on middle ring
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (PI / 180f)
                    val nodeX = center.x + (middleRadius * cos(angle)).toFloat()
                    val nodeY = center.y + (middleRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = ArcReactorCyan,
                        radius = 2.5.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // 4. Inner Core Holographic Reactor
            val coreRadius = currentRadius * 0.44f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        ArcReactorCyan,
                        ArcReactorPlasma.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Inner Ring Accent
            drawCircle(
                color = ArcReactorCyan,
                radius = coreRadius * 0.55f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
