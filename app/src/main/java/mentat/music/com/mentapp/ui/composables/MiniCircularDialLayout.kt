package mentat.music.com.mentapp.ui.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
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
    val targetAngleRad = (PI / 2.0).toFloat()
    val visualShift = (PI / 2.0).toFloat()

    // --- MOTORES DE ANIMACIÓN (Heredados del dial grande) ---
    val scannerTransition = rememberInfiniteTransition(label = "mini_scannerAnim")

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

    val innerStrokeWidth by scannerTransition.animateFloat(
        initialValue = 2f, targetValue = 5f, // Un poco más finos al ser el dial pequeño
        animationSpec = infiniteRepeatable(tween(SCANNER_INNER_DURATION + 150, easing = LinearEasing), RepeatMode.Reverse),
        label = "innerRingStroke"
    )

    val outerStrokeWidth by scannerTransition.animateFloat(
        initialValue = 2.5f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(SCANNER_OUTER_DURATION - 100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "outerRingStroke"
    )

    Layout(
        modifier = modifier,
        content = {
            val angleStep = if (items.isNotEmpty()) (2 * PI / items.size).toFloat() else 0f
            val logicOffset = (targetAngleRad - visualShift) - currentRotation
            val rawIndex = (logicOffset / angleStep).roundToInt()

            val activeIndex = if (items.isNotEmpty()) {
                var index = rawIndex % items.size
                if (index < 0) index += items.size
                index
            } else {
                -1
            }

            items.forEachIndexed { index, item ->
                val isActive = (index == activeIndex)

                val coroutineScope = rememberCoroutineScope()
                val interactionSource = remember(item.id) { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // --- ANIMACIÓN DE TAMAÑO (Con efecto hundimiento) ---
                val targetSize = when {
                    isActive && isPressed -> 50.dp // Tamaño hundido al pulsar
                    isActive -> 60.dp              // Tamaño normal activo
                    else -> 40.dp                  // Tamaño inactivo
                }

                val animatedSize by animateDpAsState(
                    targetValue = targetSize,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "size"
                )

                // --- ANIMACIÓN DE OPACIDAD ---
                val targetAlpha = if (isActive) 1f else 0.6f
                val animatedAlpha by animateFloatAsState(targetAlpha, label = "alpha")

                val painter: Painter = when (item.icon) {
                    is Int -> painterResource(id = item.icon)
                    is ImageVector -> rememberVectorPainter(item.icon)
                    else -> painterResource(id = android.R.drawable.ic_menu_help)
                }

                // El contenedor es un poco más grande que el icono para dar espacio a los anillos
                val containerSize = animatedSize + 40.dp

                Box(
                    modifier = Modifier
                        .size(containerSize)
                        .alpha(animatedAlpha)
                        .clip(CircleShape)
                        .then(
                            if (isActive) {
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

                    // --- DIBUJO DE LOS ANILLOS (Solo si está activo) ---
                    if (isActive) {
                        Canvas(modifier = Modifier.size(animatedSize * 1.4f)) {
                            val maxRadius = size.minDimension / 2 * 0.9f
                            val ringColor = Color(0xFF893471) // Color morado fijo solicitado

                            drawCircle(
                                color = ringColor.copy(alpha = 0.8f),
                                radius = maxRadius * innerScaleAnim,
                                style = Stroke(width = innerStrokeWidth.dp.toPx())
                            )

                            drawCircle(
                                color = ringColor.copy(alpha = 0.5f),
                                radius = maxRadius * outerScaleAnim,
                                style = Stroke(width = outerStrokeWidth.dp.toPx())
                            )
                        }
                    }

                    // --- EL ICONO REAL ---
                    Icon(
                        painter = painter,
                        contentDescription = item.label,
                        tint = Color.Black, // Siempre negro
                        modifier = Modifier.size(animatedSize * 0.6f)
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
                val angle = (currentAngleStep * index) + currentRotation + visualShift
                val x = (centerX + radiusPx * cos(angle.toDouble())).toInt() - (placable.width / 2)
                val y = (centerY + radiusPx * sin(angle.toDouble())).toInt() - (placable.height / 2)
                placable.placeRelative(x, y)
            }
        }
    }
}