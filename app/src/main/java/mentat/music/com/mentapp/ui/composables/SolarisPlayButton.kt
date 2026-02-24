package mentat.music.com.mentapp.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mentat.music.com.mentapp.R

@Composable
fun SolarisPlayButton(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isProcessing: Boolean = false,
    processingDuration: Int = 1000, // Nuevo parámetro para controlar el tiempo del giro
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed || isProcessing) 0.85f else 1f

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "buttonScale"
    )

    val iconRotation = remember { Animatable(0f) }

    // El giro ahora dura exactamente lo que le digamos desde fuera
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            iconRotation.animateTo(
                targetValue = iconRotation.value + 360f,
                animationSpec = tween(
                    durationMillis = processingDuration,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    val glassAlphaEnd = if (isProcessing) 0.6f else 0.9f

    val glassGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f),
            Color.White.copy(alpha = glassAlphaEnd)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .background(brush = glassGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isProcessing
            ) { onClick() }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_cambio_dial),
            contentDescription = "Action",
            tint = Color.Black.copy(alpha = if (isProcessing) 0.5f else 0.8f),
            modifier = Modifier
                .size(size * 0.4f)
                .graphicsLayer {
                    rotationZ = iconRotation.value
                }
        )
    }
}