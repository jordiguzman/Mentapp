package mentat.music.com.mentapp.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SolarisPlayButton(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animación leve de tamaño al pulsar
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

    // LÓGICA CORREGIDA:
    // Igual que el Dial -> De Izquierda (Transparente) a Derecha (Sólido)
    val glassGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f), // Izquierda: Más transparente
            Color.White.copy(alpha = 0.9f)   // Derecha: Más sólido
        ),
        start = Offset(0f, 0f),                      // Punto de inicio: Borde Izquierdo
        end = Offset(Float.POSITIVE_INFINITY, 0f)    // Punto final: Borde Derecho
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
            // 1. EL BORDE
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            // 2. EL CRISTAL (Ahora fluye igual que el anillo)
            .background(brush = glassGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        // 3. EL ICONO (Negro)
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Action",
            tint = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.size(size * 0.4f)
        )
    }
}