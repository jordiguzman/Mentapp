package mentat.music.com.mentapp.ui.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MiniCircularDialLayout(
    modifier: Modifier = Modifier,
    items: List<DialItem>,
    currentRotation: Float,
    radius: Dp
) {
    val radiusPx = with(LocalDensity.current) { radius.toPx() }

    // --- CONFIGURACIÓN DE POSICIÓN ---
    // 90 grados (PI/2) es matemáticamente "Abajo" (6 en punto) en Android UI.
    // Si los ves a la derecha, cambia esto a 0.0. Si es arriba, -PI/2.
    val targetAngleRad = (PI / 2.0).toFloat()

    Layout(
        modifier = modifier,
        content = {
            // LÓGICA DEL ELEGIDO (Highlander V2 - A prueba de vueltas)
            // Calculamos el índice basándonos en la posición VISUAL actual.
            val angleStep = if (items.isNotEmpty()) (2 * PI / items.size).toFloat() else 0f

            // 1. ¿A qué distancia angular estamos del objetivo?
            val diff = targetAngleRad - currentRotation

            // 2. ¿Cuántos pasos de 'angleStep' hay en esa distancia?
            // Usamos roundToInt para encontrar el "slot" más cercano.
            val stepsRaw = (diff / angleStep).roundToInt()

            // 3. Normalizamos el índice para que siempre esté entre 0 y (size-1)
            // Esto arregla el problema de los números negativos o vueltas extra.
            val activeIndex = if (items.isNotEmpty()) {
                var index = stepsRaw % items.size
                if (index < 0) index += items.size
                index
            } else {
                -1
            }

            items.forEachIndexed { index, item ->
                // AHORA SÍ: El que esté matemáticamente más cerca del 90º gana.
                val isActive = (index == activeIndex)

                // ESTILOS (Alto contraste para confirmar el foco)
                val targetSize = if (isActive) 52.dp else 34.dp
                val animatedSize by animateDpAsState(targetSize, spring(stiffness = Spring.StiffnessLow), label = "size")
                val targetAlpha = if (isActive) 1f else 0.3f
                val animatedAlpha by animateFloatAsState(targetAlpha, label = "alpha")

                val painter: Painter = when (item.icon) {
                    is Int -> painterResource(id = item.icon)
                    is ImageVector -> rememberVectorPainter(item.icon)
                    else -> painterResource(id = android.R.drawable.ic_menu_help)
                }

                Box(
                    modifier = Modifier
                        .size(animatedSize + 12.dp)
                        .clip(CircleShape)
                        .alpha(animatedAlpha)
                        .clickable { item.onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painter,
                        contentDescription = item.label,
                        tint = Color.Black,
                        modifier = Modifier.size(animatedSize)
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val placables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val centerX = width / 2
        val centerY = height / 2

        layout(width, height) {
            val currentAngleStep = if (items.isNotEmpty()) (2 * PI / items.size).toFloat() else 0f
            placables.forEachIndexed { index, placable ->
                // POSICIONAMIENTO ESTÁNDAR
                // 0 grados = Derecha (3 en punto)
                // 90 grados = Abajo (6 en punto)
                val angle = (currentAngleStep * index) + currentRotation
                val x = (centerX + radiusPx * cos(angle.toDouble())).toInt() - (placable.width / 2)
                val y = (centerY + radiusPx * sin(angle.toDouble())).toInt() - (placable.height / 2)
                placable.placeRelative(x, y)
            }
        }
    }
}