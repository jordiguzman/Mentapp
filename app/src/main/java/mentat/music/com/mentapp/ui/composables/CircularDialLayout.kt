package mentat.music.com.mentapp.ui.composables

import android.os.Build
import androidx.compose.animation.core.*
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

// CLASE DE DATOS (Sin cambios)
data class DialItem(
    val id: String,
    val label: String,
    val icon: Any,
    val color: Color = Color.White,
    val onClick: () -> Unit
)

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

    // --- ANIMACIONES DEL PULSO (Sin cambios) ---
    val infiniteTransition = rememberInfiniteTransition(label = "dial_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "pulseAlpha"
    )

    val angleStep = if (items.isNotEmpty()) (2 * Math.PI.toFloat() / items.size) else 0f
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    Layout(
        modifier = modifier,
        content = {
            items.forEachIndexed { index, item ->

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

                // ¿Está en foco? (Tolerancia de 0.2 radianes para que enganche bien)
                val isActive = (diff < 0.2f || abs(diff - 2 * Math.PI.toFloat()) < 0.2f)
                val isClickedIcon = (index == clickedIconIndex)

                // --- 1. ANIMACIÓN DE TAMAÑO (MÁS CONTRASTE) 📏 ---
                val targetIconSize = when {
                    isAnimatingOut && isClickedIcon -> 1000.dp
                    isAnimatingOut && !isClickedIcon -> 48.dp // Se quedan pequeños al salir
                    isActive -> 70.dp // ANTES: 64.dp -> AHORA: 80.dp (¡Gigante!)
                    else -> 42.dp     // ANTES: 48.dp -> AHORA: 40.dp (Más pequeñitos)
                }

                // Usamos spring para que rebote un poco al crecer
                val animatedIconSize by animateDpAsState(
                    targetValue = targetIconSize,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                    label = "iconSize"
                )

                // --- 2. ANIMACIÓN DE OPACIDAD (FONDO VS FRENTE) 👻 ---
                val targetAlpha = when {
                    isAnimatingOut -> 0f
                    isActive -> 1.0f // Foco total
                    else -> 0.4f     // Los otros se ven muy tenues (Ghosted)
                }
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(300),
                    label = "alpha"
                )

                val containerSize = animatedIconSize + 80.dp // Espacio extra para sombras/halos

                // --- CONTENEDOR DEL ÍTEM ---
                Box(
                    modifier = Modifier
                        .alpha(animatedAlpha) // Aplicamos la transparencia aquí
                        .size(containerSize)
                        .clip(CircleShape)
                        .then(if (isActive && !isAnimatingOut) Modifier.clickable { item.onClick() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {

                    // A) EL HALO DE COLOR (Solo si está activo)
                    if (isActive && !isAnimatingOut) {
                        Box(
                            modifier = Modifier
                                .size(animatedIconSize)
                                .scale(pulseScale)
                                .background(
                                    color = item.color.copy(alpha = pulseAlpha),
                                    shape = CircleShape
                                )
                        )
                    }

                    // B) SOMBRA
                    if (!isAnimatingOut) {
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(animatedIconSize)
                                .scale(0.85f)
                                .offset(x = 6.dp, y = 6.dp)
                                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(4.dp) else Modifier)
                        )
                    }

                    // C) ICONO REAL
                    Box(modifier = Modifier.size(animatedIconSize)) {
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
        // Layout sin cambios
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