package mentat.music.com.mentapp.ui.composables

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
    items: List<DialItem>,
    currentRotation: Float,
    iconPathRadius: Dp,
    isAnimatingOut: Boolean,
    clickedIconIndex: Int,
    isExpansionFinished: Boolean,
) {
    val radiusPx = with(LocalDensity.current) { iconPathRadius.toPx() }

    // --- 1. EL MOTOR DEL LATIDO (Propio del Dial) ---
    // Lo definimos aquí para que funcione autónomamente.
    // Todos los iconos usarán este mismo ritmo cuando les toque estar activos.
    val infiniteTransition = rememberInfiniteTransition(label = "dial_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f, // Crece hasta un 40% más
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing), // Un poco más rápido que el radar
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, // Opacidad inicial
        targetValue = 0.3f,  // Se desvanece
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Calculamos el paso del ángulo
    val angleStep = if (items.isNotEmpty()) (2 * Math.PI.toFloat() / items.size) else 0f
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    Layout(
        modifier = modifier,
        content = {
            items.forEachIndexed { index, item ->

                // --- LÓGICA DE PINTOR ---
                val painter: Painter = when (item.icon) {
                    is Int -> painterResource(id = item.icon)
                    is ImageVector -> rememberVectorPainter(item.icon)
                    else -> painterResource(id = android.R.drawable.ic_menu_help)
                }

                // --- CÁLCULO DE ÁNGULO Y FOCO ---
                val angle = (angleStep * index) + currentRotation
                val normalizedAngle = (angle % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val targetAngleNorm = (targetAngleRad % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val diff = abs(normalizedAngle - targetAngleNorm)

                // ¿Está este ítem en la posición principal (arriba/derecha)?
                val isActive = (diff < 0.05f || abs(diff - 2 * Math.PI.toFloat()) < 0.05f)
                val isClickedIcon = (index == clickedIconIndex)

                // --- ANIMACIÓN DE TAMAÑO ---
                val targetIconSize = when {
                    isAnimatingOut && isClickedIcon -> 1000.dp
                    isAnimatingOut && !isClickedIcon -> 48.dp
                    isActive -> 64.dp // Cuando está activo es más grande
                    else -> 48.dp
                }
                val animatedIconSize by animateDpAsState(
                    targetValue = targetIconSize,
                    animationSpec = tween(durationMillis = 300),
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
                    animationSpec = tween(durationMillis = 300),
                    label = "alpha"
                )

                // --- CONTENEDOR PRINCIPAL ---
                Box(
                    modifier = Modifier
                        .alpha(containerAnimatedAlpha)
                        .size(containerSize)
                        // IMPORTANTE: Si recortamos con clip(CircleShape) aquí,
                        // el halo se cortará si es más grande que containerSize.
                        // Como containerSize tiene "extraSpaceForShadow", debería caber bien.
                        .clip(CircleShape)
                        .then(if (isActive && !isAnimatingOut) Modifier.clickable {
                            item.onClick()
                        } else Modifier),
                    contentAlignment = Alignment.Center
                ) {

                    // 2. --- EL HALO DE COLOR (NUEVO) --- 🔴🟢🔵
                    // Solo se dibuja si el ítem está ACTIVO (en foco)
                    if (isActive && !isAnimatingOut) {
                        Box(
                            modifier = Modifier
                                .size(animatedIconSize) // Base del tamaño del icono
                                .scale(pulseScale)      // Crece y decrece
                                .background(
                                    // Usamos el color del ítem con la transparencia animada
                                    color = item.color.copy(alpha = pulseAlpha),
                                    shape = CircleShape
                                )
                        )
                    }

                    // 3. --- SOMBRA ---
                    if (!isAnimatingOut) {
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(animatedIconSize)
                                .scale(shadowScale)
                                .offset(x = 6.dp, y = 6.dp)
                                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(4.dp) else Modifier)
                        )
                    }

                    // 4. --- ICONO ---
                    Box(modifier = Modifier.size(animatedIconSize)) {
                        // Truco para tintar si es Vector, o dejar original si es PNG
                        val iconTint = if (item.icon is ImageVector) item.color else Color.Unspecified

                        Icon(
                            painter = painter,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        // --- LAYOUT (Sin cambios) ---
        val placables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val centerX = layoutWidth / 2
        val centerY = layoutHeight / 2
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
