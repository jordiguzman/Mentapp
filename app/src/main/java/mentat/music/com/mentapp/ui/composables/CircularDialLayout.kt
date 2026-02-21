package mentat.music.com.mentapp.ui.composables

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.ui.screens.home.DialConstants.SCANNER_INNER_DURATION
import mentat.music.com.mentapp.ui.screens.home.DialConstants.SCANNER_OUTER_DURATION
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// CLASE DE DATOS
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

    val scannerTransition = rememberInfiniteTransition(label = "scannerAnim")

    // --- 1. ANIMACIONES DE ESCALA (Radio) ---
    val innerScaleAnim by scannerTransition.animateFloat(
        initialValue = 0.65f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(SCANNER_INNER_DURATION, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "innerRingScale"
    )

    val outerScaleAnim by scannerTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(SCANNER_OUTER_DURATION, easing = LinearEasing), RepeatMode.Reverse),
        label = "outerRingScale"
    )

    // --- 2. ANIMACIONES DE GROSOR (Variable) ---
    // Tiempos ligeramente desfasados de la escala para que la respiración no sea robótica
    val innerStrokeWidth by scannerTransition.animateFloat(
        initialValue = 2f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(SCANNER_INNER_DURATION + 150, easing = LinearEasing), RepeatMode.Reverse),
        label = "innerRingStroke"
    )

    val outerStrokeWidth by scannerTransition.animateFloat(
        initialValue = 3f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(SCANNER_OUTER_DURATION - 100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "outerRingStroke"
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

                val angle = (angleStep * index) + currentRotation
                val normalizedAngle = (angle % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val targetAngleNorm = (targetAngleRad % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val diff = abs(normalizedAngle - targetAngleNorm)

                val isActive = (diff < 0.2f || abs(diff - 2 * Math.PI.toFloat()) < 0.2f)
                val isClickedIcon = (index == clickedIconIndex)

                val coroutineScope = rememberCoroutineScope()
                val interactionSource = remember(item.id) { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // --- 3. EL EFECTO DE HUNDIMIENTO ---
                val targetIconSize = when {
                    isAnimatingOut && isClickedIcon -> 1000.dp
                    isAnimatingOut && !isClickedIcon -> 48.dp
                    isActive && isPressed -> 55.dp // Tamaño hundido al pulsar
                    isActive -> 70.dp              // Tamaño normal en foco
                    else -> 42.dp
                }

                val animatedIconSize by animateDpAsState(
                    targetValue = targetIconSize,
                    animationSpec = if (isAnimatingOut) {
                        // Usa los 900ms de tus constantes para la expansión gigante
                        tween(mentat.music.com.mentapp.ui.screens.home.DialConstants.TRANSITION_DURATION, easing = FastOutSlowInEasing)
                    } else {
                        // Usa el rebote rápido físico para el click normal
                        spring(dampingRatio = 0.6f, stiffness = 400f)
                    },
                    label = "iconSize"
                )

                val targetAlpha = when {
                    isAnimatingOut -> 0f
                    isActive -> 1.0f
                    else -> 0.4f
                }
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(300),
                    label = "alpha"
                )

                val containerSize = animatedIconSize + 60.dp

                Box(
                    modifier = Modifier
                        .alpha(animatedAlpha)
                        .size(containerSize)
                        .clip(CircleShape)
                        .then(
                            if (isActive && !isAnimatingOut) {
                                Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        delay(300)
                                        item.onClick()
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    if (isActive && !isAnimatingOut) {
                        Canvas(modifier = Modifier.size(animatedIconSize * 1.5f)) {
                            val maxRadius = size.minDimension / 2 * 0.9f

                            // Anillo Interior (Círculo completo con grosor variable)
                            drawCircle(
                                color = item.color.copy(alpha = 0.8f),
                                radius = maxRadius * innerScaleAnim,
                                style = Stroke(width = innerStrokeWidth.dp.toPx())
                            )

                            // Anillo Exterior (Círculo completo con grosor variable)
                            drawCircle(
                                color = item.color.copy(alpha = 0.5f),
                                radius = maxRadius * outerScaleAnim,
                                style = Stroke(width = outerStrokeWidth.dp.toPx())
                            )
                        }
                    }

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