package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CandyColor
import com.example.data.model.CandyItem
import com.example.data.model.SpecialType
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CandyView(
    candy: CandyItem?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            )
            .testTag("candy_${candy?.color}_${candy?.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (candy != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = if (isSelected) pulseScale else 1.0f
                val w = size.width * scale
                val h = size.height * scale
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                // Draw candy body based on color & shape
                drawCandyBody(candy.color, candy.special, centerX, centerY, w, h)

                // Draw Special overlays (Striped, Wrapped, Color Bomb)
                drawSpecialEffects(candy.special, centerX, centerY, w, h)

                // Selection glowing halo
                if (isSelected) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = (w / 2f).coerceAtMost(h / 2f) * 0.95f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCandyBody(
    color: CandyColor,
    special: SpecialType,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float
) {
    if (special == SpecialType.COLOR_BOMB) {
        // Shimmering rainbow chocolate disco ball
        val rainbowBrush = Brush.sweepGradient(
            listOf(
                Color(0xFFFF0055),
                Color(0xFFFF9900),
                Color(0xFFFFEE00),
                Color(0xFF00EE77),
                Color(0xFF00AAFF),
                Color(0xFF9900FF),
                Color(0xFFFF0055)
            ),
            center = Offset(cx, cy)
        )
        val radius = w * 0.38f
        drawCircle(brush = rainbowBrush, radius = radius, center = Offset(cx, cy))
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = radius * 0.5f,
            center = Offset(cx - radius * 0.25f, cy - radius * 0.25f)
        )
        // Sprinkles
        val sprinkleColors = listOf(Color.White, Color.Yellow, Color.Cyan)
        for (i in 0 until 5) {
            val angle = i * (Math.PI * 2 / 5).toFloat()
            val sx = cx + cos(angle) * (radius * 0.5f)
            val sy = cy + sin(angle) * (radius * 0.5f)
            drawCircle(color = sprinkleColors[i % sprinkleColors.size], radius = 3.dp.toPx(), center = Offset(sx, sy))
        }
        return
    }

    val baseColor = when (color) {
        CandyColor.RED -> CandyRed
        CandyColor.ORANGE -> CandyOrange
        CandyColor.YELLOW -> CandyYellow
        CandyColor.GREEN -> CandyGreen
        CandyColor.BLUE -> CandyBlue
        CandyColor.PURPLE -> CandyPurple
    }

    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.8f),
            baseColor,
            baseColor.copy(alpha = 0.85f)
        ),
        center = Offset(cx - w * 0.12f, cy - h * 0.12f),
        radius = w * 0.5f
    )

    when (color) {
        CandyColor.RED -> {
            // Heart shape
            val path = Path().apply {
                val r = w * 0.35f
                moveTo(cx, cy + r * 0.8f)
                cubicTo(cx - r * 1.2f, cy, cx - r * 1.2f, cy - r * 0.8f, cx - r * 0.5f, cy - r * 0.8f)
                cubicTo(cx, cy - r * 0.8f, cx, cy - r * 0.3f, cx, cy - r * 0.3f)
                cubicTo(cx, cy - r * 0.3f, cx, cy - r * 0.8f, cx + r * 0.5f, cy - r * 0.8f)
                cubicTo(cx + r * 1.2f, cy - r * 0.8f, cx + r * 1.2f, cy, cx, cy + r * 0.8f)
                close()
            }
            drawPath(path, brush = gradientBrush, style = Fill)
        }
        CandyColor.ORANGE -> {
            // Round lozenge / sphere
            val radius = w * 0.36f
            drawCircle(brush = gradientBrush, radius = radius, center = Offset(cx, cy))
            // Inner spiral groove
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = radius * 0.4f,
                center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        CandyColor.YELLOW -> {
            // Rounded sweet cube
            val rectSize = w * 0.65f
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(cx - rectSize / 2, cy - rectSize / 2),
                size = Size(rectSize, rectSize),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
        }
        CandyColor.GREEN -> {
            // Teardrop / Hexagon sweet
            val rectSize = w * 0.70f
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(cx - rectSize / 2, cy - rectSize / 2),
                size = Size(rectSize, rectSize),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            )
        }
        CandyColor.BLUE -> {
            // Star candy
            val path = Path()
            val outerR = w * 0.38f
            val innerR = outerR * 0.5f
            for (i in 0 until 5) {
                val outerAngle = (i * 72 - 90) * (Math.PI / 180).toFloat()
                val innerAngle = ((i * 72) + 36 - 90) * (Math.PI / 180).toFloat()
                val ox = cx + cos(outerAngle) * outerR
                val oy = cy + sin(outerAngle) * outerR
                val ix = cx + cos(innerAngle) * innerR
                val iy = cy + sin(innerAngle) * innerR
                if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
                path.lineTo(ix, iy)
            }
            path.close()
            drawPath(path, brush = gradientBrush, style = Fill)
        }
        CandyColor.PURPLE -> {
            // Berry oval drop
            drawOval(
                brush = gradientBrush,
                topLeft = Offset(cx - w * 0.35f, cy - h * 0.32f),
                size = Size(w * 0.70f, h * 0.64f)
            )
        }
    }

    // Top-left gloss gleam
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = w * 0.08f,
        center = Offset(cx - w * 0.16f, cy - h * 0.16f)
    )
}

private fun DrawScope.drawSpecialEffects(
    special: SpecialType,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float
) {
    when (special) {
        SpecialType.STRIPED_HORIZONTAL -> {
            val stripeColor = Color.White.copy(alpha = 0.75f)
            val strokeW = 4.dp.toPx()
            drawLine(stripeColor, Offset(cx - w * 0.35f, cy - h * 0.12f), Offset(cx + w * 0.35f, cy - h * 0.12f), strokeW)
            drawLine(stripeColor, Offset(cx - w * 0.38f, cy), Offset(cx + w * 0.38f, cy), strokeW)
            drawLine(stripeColor, Offset(cx - w * 0.35f, cy + h * 0.12f), Offset(cx + w * 0.35f, cy + h * 0.12f), strokeW)
        }
        SpecialType.STRIPED_VERTICAL -> {
            val stripeColor = Color.White.copy(alpha = 0.75f)
            val strokeW = 4.dp.toPx()
            drawLine(stripeColor, Offset(cx - w * 0.12f, cy - h * 0.35f), Offset(cx - w * 0.12f, cy + h * 0.35f), strokeW)
            drawLine(stripeColor, Offset(cx, cy - h * 0.38f), Offset(cx, cy + h * 0.38f), strokeW)
            drawLine(stripeColor, Offset(cx + w * 0.12f, cy - h * 0.35f), Offset(cx + w * 0.12f, cy + h * 0.35f), strokeW)
        }
        SpecialType.WRAPPED -> {
            // Sparkling translucent ribbon wrapper
            val ribbonColor = Color.White.copy(alpha = 0.85f)
            drawRoundRect(
                color = ribbonColor,
                topLeft = Offset(cx - w * 0.42f, cy - h * 0.42f),
                size = Size(w * 0.84f, h * 0.84f),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(color = Color(0xFFFFD700), radius = 5.dp.toPx(), center = Offset(cx, cy))
        }
        SpecialType.COLOR_BOMB, SpecialType.NONE -> {}
    }
}
