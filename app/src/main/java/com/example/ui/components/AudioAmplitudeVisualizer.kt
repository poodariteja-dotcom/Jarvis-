package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.ArcReactorPlasma
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextDim
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerStyle {
    CENTER_MIRRORED,
    CONTINUOUS_WAVE,
    FREQUENCY_BARS
}

/**
 * High-performance Jetpack Compose audio amplitude visualizer component.
 * Displays real-time audio amplitudes during recording with dynamic waveforms,
 * glowing neon holographic HUD shaders, and decibel responsiveness.
 */
@Composable
fun AudioAmplitudeVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    style: VisualizerStyle = VisualizerStyle.CENTER_MIRRORED,
    height: Dp = 56.dp,
    showTelemetryBadge: Boolean = true
) {
    // Smoothed amplitude animation so bars glide smoothly between microphone buffers
    val smoothedAmplitude = remember { Animatable(0f) }
    LaunchedEffect(amplitude, isRecording) {
        val target = if (isRecording) amplitude.coerceIn(0f, 1f) else 0f
        smoothedAmplitude.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (target > smoothedAmplitude.value) 60 else 180,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Rolling historical amplitude buffer for organic ripple waves
    val amplitudeHistory = remember {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0f) }
        }
    }

    LaunchedEffect(smoothedAmplitude.value) {
        if (amplitudeHistory.size >= barCount) {
            amplitudeHistory.removeAt(0)
        }
        amplitudeHistory.add(smoothedAmplitude.value)
    }

    // Continuous subtle oscillation so the HUD feels alive even in moments of silence
    val infiniteTransition = rememberInfiniteTransition(label = "hud_ambient_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_amplitude_visualizer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Telemetry header if enabled
        if (showTelemetryBadge && isRecording) {
            Row(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurface.copy(alpha = 0.8f))
                    .border(0.5.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (smoothedAmplitude.value > 0.6f) ArcReactorGold else ArcReactorCyan)
                )
                Text(
                    text = "MIC LIVE • ${(smoothedAmplitude.value * 100).toInt()}% GAIN",
                    color = ArcReactorCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Canvas Waveform Renderer
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .testTag("amplitude_canvas")
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            when (style) {
                VisualizerStyle.CENTER_MIRRORED -> {
                    drawCenterMirroredVisualizer(
                        amplitudeHistory = amplitudeHistory,
                        currentAmp = smoothedAmplitude.value,
                        isRecording = isRecording,
                        phase = phase,
                        barCount = barCount,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        centerY = centerY
                    )
                }
                VisualizerStyle.CONTINUOUS_WAVE -> {
                    drawContinuousSineWave(
                        currentAmp = smoothedAmplitude.value,
                        isRecording = isRecording,
                        phase = phase,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        centerY = centerY,
                        glowPulse = glowPulse
                    )
                }
                VisualizerStyle.FREQUENCY_BARS -> {
                    drawFrequencyBars(
                        amplitudeHistory = amplitudeHistory,
                        currentAmp = smoothedAmplitude.value,
                        isRecording = isRecording,
                        phase = phase,
                        barCount = barCount,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCenterMirroredVisualizer(
    amplitudeHistory: List<Float>,
    currentAmp: Float,
    isRecording: Boolean,
    phase: Float,
    barCount: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    centerY: Float
) {
    val barSlotWidth = canvasWidth / barCount
    val barWidth = (barSlotWidth * 0.55f).coerceIn(2.5f, 10f)
    val halfCount = barCount / 2

    // Center baseline glow line
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                ArcReactorCyan.copy(alpha = 0.25f),
                ArcReactorCyan.copy(alpha = 0.7f),
                ArcReactorCyan.copy(alpha = 0.25f),
                Color.Transparent
            )
        ),
        start = Offset(0f, centerY),
        end = Offset(canvasWidth, centerY),
        strokeWidth = 1.dp.toPx()
    )

    for (i in 0 until barCount) {
        val distanceFromCenter = abs(i - halfCount).toFloat() / halfCount
        val bellCurve = (1f - distanceFromCenter * 0.7f).coerceAtLeast(0.15f)

        // Historical sample weighting
        val historyIdx = (i * amplitudeHistory.size / barCount).coerceIn(0, amplitudeHistory.lastIndex)
        val historyAmp = amplitudeHistory.getOrElse(historyIdx) { 0f }

        // Ambient idle undulation
        val ambientWave = sin(distanceFromCenter * 5f - phase) * 0.08f

        val activeAmp = if (isRecording) {
            (historyAmp * 0.65f + currentAmp * 0.35f) * bellCurve + ambientWave
        } else {
            ambientWave.coerceAtLeast(0.02f)
        }

        val normalizedHeight = activeAmp.coerceIn(0.04f, 1.0f)
        val barHeight = (canvasHeight * normalizedHeight).coerceAtLeast(4f)
        val x = i * barSlotWidth + (barSlotWidth - barWidth) / 2f
        val y = centerY - (barHeight / 2f)

        val isPeak = currentAmp > 0.65f && distanceFromCenter < 0.25f
        val barColor = when {
            isPeak -> ArcReactorGold
            distanceFromCenter < 0.3f -> ArcReactorCyan
            distanceFromCenter < 0.6f -> ArcReactorPlasma
            else -> ArcReactorCyan.copy(alpha = 0.6f)
        }

        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

private fun DrawScope.drawContinuousSineWave(
    currentAmp: Float,
    isRecording: Boolean,
    phase: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    centerY: Float,
    glowPulse: Float
) {
    val points = 60
    val step = canvasWidth / points
    val maxWaveAmp = (canvasHeight * 0.42f) * (if (isRecording) (0.25f + currentAmp * 0.75f) else 0.08f)

    for (layer in 0..2) {
        val layerPhase = phase + (layer * 1.1f)
        val layerAlpha = when (layer) {
            0 -> 0.9f
            1 -> 0.55f
            else -> 0.3f
        }
        val layerColor = if (layer == 0) ArcReactorCyan else ArcReactorPlasma

        var prevX = 0f
        var prevY = centerY

        for (i in 0..points) {
            val x = i * step
            val normalizedX = (i.toFloat() / points) * 2f * Math.PI.toFloat()
            val envelope = sin(i.toFloat() / points * Math.PI.toFloat()) // Tapers at left/right edges
            val wave = sin(normalizedX * 2.5f + layerPhase) * cos(normalizedX * 1.2f + layerPhase * 0.5f)
            val y = centerY + (wave * maxWaveAmp * envelope)

            if (i > 0) {
                drawLine(
                    color = layerColor.copy(alpha = layerAlpha * glowPulse),
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = (2.5f - layer * 0.6f).dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            prevX = x
            prevY = y
        }
    }
}

private fun DrawScope.drawFrequencyBars(
    amplitudeHistory: List<Float>,
    currentAmp: Float,
    isRecording: Boolean,
    phase: Float,
    barCount: Int,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val barSlotWidth = canvasWidth / barCount
    val barWidth = (barSlotWidth * 0.7f).coerceIn(2.5f, 12f)

    for (i in 0 until barCount) {
        val historyIdx = (i * amplitudeHistory.size / barCount).coerceIn(0, amplitudeHistory.lastIndex)
        val sample = amplitudeHistory.getOrElse(historyIdx) { 0f }
        val ripple = (sin(i * 0.35f + phase) + 1f) / 2f
        val amp = if (isRecording) (sample * 0.75f + currentAmp * 0.25f * ripple) else (0.05f * ripple)

        val barHeight = (canvasHeight * amp.coerceIn(0.05f, 1f)).coerceAtLeast(3f)
        val x = i * barSlotWidth + (barSlotWidth - barWidth) / 2f
        val y = canvasHeight - barHeight

        val gradient = Brush.verticalGradient(
            colors = listOf(
                ArcReactorCyan,
                ArcReactorPlasma.copy(alpha = 0.8f),
                ArcReactorCyan.copy(alpha = 0.3f)
            ),
            startY = y,
            endY = canvasHeight
        )

        drawRoundRect(
            brush = gradient,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}
