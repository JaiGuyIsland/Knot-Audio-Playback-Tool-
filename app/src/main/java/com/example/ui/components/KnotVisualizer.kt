package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KnotAccent
import com.example.ui.theme.KnotAccentPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KnotVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    sizeDp: Dp = 220.dp,
    accentColor: Color = KnotAccent,
    secondaryColor: Color = KnotAccentPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "knot_anim")

    // Slow organic rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 16000 else 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isPlaying) 1.06f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 2200 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Wave phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 3500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2.7f) * pulseScale

            val rotRad = Math.toRadians(rotationAngle.toDouble())
            val cosRot = cos(rotRad).toFloat()
            val sinRot = sin(rotRad).toFloat()

            // 1. Ambient Background Loop Glow
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isPlaying) 0.18f else 0.08f),
                    secondaryColor.copy(alpha = if (isPlaying) 0.08f else 0.03f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.5f
            )
            drawCircle(brush = glowBrush, radius = radius * 1.5f, center = center)

            // 2. Parametric Knot (Lemniscate with harmonic wobble)
            val steps = 300
            val knotPath = Path()
            val innerPath = Path()

            for (i in 0..steps) {
                val t = (2.0 * PI * i / steps)
                val denom = 1.0 + (sin(t) * sin(t))
                val rawX = (radius * cos(t) / denom).toFloat()
                val rawY = (radius * sin(t) * cos(t) * 1.55 / denom).toFloat()

                // Add harmonic ripple when playing
                val wobble = if (isPlaying) (sin(3 * t + wavePhase) * 6f).toFloat() else 0f
                val wx = rawX + (rawX / (radius + 0.1f)) * wobble
                val wy = rawY + (rawY / (radius + 0.1f)) * wobble

                // Rotate around center
                val rotatedX = center.x + (wx * cosRot - wy * sinRot)
                val rotatedY = center.y + (wx * sinRot + wy * cosRot)

                if (i == 0) {
                    knotPath.moveTo(rotatedX, rotatedY)
                } else {
                    knotPath.lineTo(rotatedX, rotatedY)
                }

                // Secondary inner ghost loop
                val innerR = 0.88f
                val ix = center.x + ((wx * innerR) * cosRot - (wy * innerR) * sinRot)
                val iy = center.y + ((wx * innerR) * sinRot + (wy * innerR) * cosRot)
                if (i == 0) {
                    innerPath.moveTo(ix, iy)
                } else {
                    innerPath.lineTo(ix, iy)
                }
            }
            knotPath.close()
            innerPath.close()

            // Draw outer ambient line
            drawPath(
                path = knotPath,
                brush = Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.3f), secondaryColor.copy(alpha = 0.3f)),
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius)
                ),
                style = Stroke(
                    width = 16f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw primary crisp knot line
            drawPath(
                path = knotPath,
                brush = Brush.linearGradient(
                    colors = listOf(accentColor, secondaryColor, accentColor),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y)
                ),
                style = Stroke(
                    width = 4.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw subtle inner thread
            drawPath(
                path = innerPath,
                color = Color.White.copy(alpha = if (isPlaying) 0.5f else 0.25f),
                style = Stroke(
                    width = 1.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
