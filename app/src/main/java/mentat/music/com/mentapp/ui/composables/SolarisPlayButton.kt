package mentat.music.com.mentapp.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mentat.music.com.mentapp.R

@Composable
fun SolarisPlayButton(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    onClick: () -> Unit
) {
    // Usamos un InteractionSource para que HomeScreen también pueda detectar la pulsación si lo necesita
    val interactionSource = remember { MutableInteractionSource() }

    // Gradiente de cristal: De Izquierda (Transparente) a Derecha (Sólido)
    val glassGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.9f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Box(
        contentAlignment = Alignment.Center,
        // Aplicamos el 'modifier' que viene de fuera PRIMERO.
        // Así, la escala que enviamos desde HomeScreen manda sobre el contenedor.
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            // 1. EL BORDE
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            // 2. EL FONDO DE CRISTAL
            .background(brush = glassGradient)
            // 3. LA INTERACCIÓN
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        // 3. EL ICONO (Negro con transparencia)
        Icon(
            painter = painterResource(id = R.drawable.ic_cambio_dial),
            contentDescription = "Action",
            tint = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.size(size * 0.4f)
        )
    }
}