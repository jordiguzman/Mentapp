package mentat.music.com.mentapp.ui.composables

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// --- NUEVA CLASE HÍBRIDA ---
// 'icon' ahora es 'Any' para aceptar R.drawable (Int) O ImageVector
data class DialItem(
    val id: String,         // Identificador único (ej: "Spotify")
    val label: String,      // Texto para accesibilidad
    val icon: Any,          // <--- EL TRUCO: Acepta Int o ImageVector
    val color: Color = Color.White, // Color de marca (opcional)
    val onClick: () -> Unit // La acción click va aquí dentro
)

// Constante de animación
private const val DIAL_TRANSITION_DURATION = 300

@Composable
fun CircularDialLayout(
    modifier: Modifier = Modifier,
    items: List<DialItem>, // <--- AHORA RECIBIMOS LA LISTA DESDE FUERA
    currentRotation: Float,
    iconPathRadius: Dp,
    isAnimatingOut: Boolean,
    clickedIconIndex: Int,
    isExpansionFinished: Boolean,
    // Eliminamos onIconClick global porque cada item trae su onClick
    // contentFor ahora es más simple, solo para pintar el icono principal
) {
    val radiusPx = with(LocalDensity.current) { iconPathRadius.toPx() }

    // Calculamos el paso del ángulo según cuantos items nos pasen
    val angleStep = if (items.isNotEmpty()) (2 * Math.PI.toFloat() / items.size) else 0f
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    Layout(
        modifier = modifier,
        content = {
            items.forEachIndexed { index, item ->

                // --- LÓGICA DE PINTOR (HÍBRIDO) ---
                // Aquí decidimos qué herramienta usar para dibujar
                val painter: Painter = when (item.icon) {
                    is Int -> painterResource(id = item.icon)           // Es un PNG/XML antiguo
                    is ImageVector -> rememberVectorPainter(item.icon)  // Es un Vector nuevo
                    else -> painterResource(id = android.R.drawable.ic_menu_help) // Fallback por seguridad
                }

                // --- LÓGICA DE ÁNGULO Y ESTADO ---
                val angle = (angleStep * index) + currentRotation
                val normalizedAngle = (angle % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val targetAngleNorm = (targetAngleRad % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val diff = abs(normalizedAngle - targetAngleNorm)
                val isActive = (diff < 0.05f || abs(diff - 2 * Math.PI.toFloat()) < 0.05f)
                val isClickedIcon = (index == clickedIconIndex)

                // --- TAMAÑOS ---
                val targetIconSize = when {
                    isAnimatingOut && isClickedIcon -> 1000.dp
                    isAnimatingOut && !isClickedIcon -> 48.dp
                    isActive -> 64.dp
                    else -> 48.dp
                }
                val animatedIconSize by animateDpAsState(
                    targetValue = targetIconSize,
                    animationSpec = tween(durationMillis = DIAL_TRANSITION_DURATION),
                    label = "iconSize"
                )

                val extraSpaceForShadow = 80.dp
                val shadowScale = 0.85f
                val containerSize = animatedIconSize + extraSpaceForShadow

                // --- ANIMACIÓN DE APARICIÓN ---
                val containerTargetAlpha = when {
                    isAnimatingOut && !isClickedIcon -> 0.0f
                    else -> 1.0f
                }
                val containerAnimatedAlpha by animateFloatAsState(
                    targetValue = containerTargetAlpha,
                    animationSpec = tween(durationMillis = DIAL_TRANSITION_DURATION),
                    label = "alpha"
                )

                // --- CAJA PRINCIPAL ---
                Box(
                    modifier = Modifier
                        .alpha(containerAnimatedAlpha)
                        .size(containerSize)
                        .then(if (isActive && !isAnimatingOut) Modifier.clickable {
                            item.onClick() // <--- Ejecutamos el click del item
                        } else Modifier),
                    contentAlignment = Alignment.Center
                ) {

                    // --- SOMBRA (Ahora usa el 'painter' híbrido) ---
                    if (!isAnimatingOut) {
                        Icon(
                            painter = painter, // Usamos el pintor calculado arriba
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(animatedIconSize)
                                .scale(shadowScale)
                                .offset(x = 6.dp, y = 6.dp)
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.blur(4.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }

                    // --- ICONO REAL ---
                    Box(modifier = Modifier.size(animatedIconSize)) {
                        // Pintamos el icono normal (sin tintar, para que tus PNGs conserven color)
                        // OJO: Si usas vectores del sistema, son negros por defecto.
                        // Aquí aplicamos un pequeño truco: si es vector, lo pintamos blanco/color.
                        // Si es PNG, lo dejamos tal cual (tint = Color.Unspecified).

                        val iconTint = if (item.icon is ImageVector) item.color else Color.Unspecified

                        Icon(
                            painter = painter,
                            contentDescription = item.label,
                            tint = iconTint, // Respetamos tus PNGs originales
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        // --- LÓGICA DE LAYOUT (Estándar circular) ---
        val placables = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val centerX = layoutWidth / 2
        val centerY = layoutHeight / 2

        // Recalculamos el paso aquí también por seguridad
        val currentAngleStep = if (items.isNotEmpty()) (2 * Math.PI.toFloat() / items.size) else 0f

        layout(layoutWidth, layoutHeight) {
            placables.forEachIndexed { index, placable ->
                val angle = (currentAngleStep * index) + currentRotation
                val x = (centerX + radiusPx * cos(angle.toDouble())).toInt() - (placable.width / 2)
                val y = (centerY + radiusPx * sin(angle.toDouble())).toInt() - (placable.height / 2)
                placable.placeRelative(x, y)
            }
        }
    }
}