package mentat.music.com.mentapp.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.ui.theme.VerdanaFontFamily
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun DialLayer(
    modifier: Modifier = Modifier,
    // Estado y Datos
    currentItems: List<DialItem>,
    isPortrait: Boolean,
    dialTitle: String,
    // Animaciones (Estados persistentes en HomeScreen/ViewModel)
    rotationAngle: Animatable<Float, AnimationVector1D>,
    dialScale: Animatable<Float, AnimationVector1D>,
    dialFlipX: Animatable<Float, AnimationVector1D>,
    dialBlur: Animatable<Float, AnimationVector1D>,
    // Flags de Control
    isAnimatingOut: Boolean,
    clickedIconIndex: Int,
    isExpansionFinished: Boolean,
    isWebMenuOpen: Boolean,
    arrowsAlpha: Float,
    // Dimensiones y Física
    iconPathRadius: Dp,
    radiusPx: Float,
    thicknessPx: Float,
    arrowsYOffset: Dp,
    angleStep: Float,
    targetAngleRad: Float,
    // Callbacks y Scope
    onRotationComplete: (Float) -> Unit,
    scope: CoroutineScope
) {
    // El Box principal ahora es el que gestiona los gestos de rotación
    Box(
        modifier = modifier
            .pointerInput(clickedIconIndex, isAnimatingOut, isExpansionFinished, isWebMenuOpen) {
                // Bloqueo de interacción si hay menús abiertos o animaciones en curso
                if (isAnimatingOut || isExpansionFinished || isWebMenuOpen) return@pointerInput

                var centerX = 0f
                var centerY = 0f

                detectDragGestures(
                    onDragStart = {
                        centerX = size.width / 2f
                        centerY = size.height / 2f
                        scope.launch { rotationAngle.stop() }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val startAngle = atan2(change.previousPosition.y - centerY, change.previousPosition.x - centerX)
                        val endAngle = atan2(change.position.y - centerY, change.position.x - centerX)

                        scope.launch {
                            rotationAngle.snapTo(rotationAngle.value + (endAngle - startAngle))
                        }
                    },
                    onDragEnd = {
                        val currentOffset = rotationAngle.value - targetAngleRad
                        val nearestIconIndex = -(currentOffset / angleStep).roundToInt()
                        val targetSnapAngle = targetAngleRad - (angleStep * nearestIconIndex)

                        scope.launch {
                            rotationAngle.animateTo(
                                targetValue = targetSnapAngle,
                                animationSpec = spring(0.7f, 100f)
                            )
                            onRotationComplete(targetSnapAngle)
                        }
                    }
                )
            }
    ) {
        // 1. Título dinámico (MENTAPP)
        Text(
            text = dialTitle,
            color = Color.White.copy(0.5f),
            fontSize = 22.sp,
            fontFamily = VerdanaFontFamily,
            modifier = Modifier
                .align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart)
                .padding(32.dp)
        )

        // 2. Grupo Visual del Dial (Donut + Flechas + Iconos)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(dialScale.value)
                .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                .graphicsLayer {
                    scaleX = dialScale.value * dialFlipX.value
                    scaleY = dialScale.value
                },
            contentAlignment = Alignment.Center
        ) {
            // A. El Donut (Dibujo en Canvas)
            Canvas(Modifier.fillMaxSize()) {
                val brush = Brush.sweepGradient(
                    listOf(
                        Color.White.copy(0.95f),
                        Color.White.copy(0.4f),
                        Color.Gray.copy(0.6f),
                        Color.White.copy(0.4f),
                        Color.White.copy(0.95f)
                    ),
                    center = center
                )
                drawCircle(brush, radiusPx, style = Stroke(thicknessPx))
                drawCircle(Color.White.copy(0.8f), radiusPx - (thicknessPx / 2), style = Stroke(1.5.dp.toPx()))
                drawCircle(Color.White.copy(0.5f), radiusPx + (thicknessPx / 2), style = Stroke(2.dp.toPx()))
            }

            // B. Flechas Indicadoras
            Row(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = arrowsYOffset)
                    .alpha(arrowsAlpha)
            ) {
                Image(
                    painter = painterResource(R.drawable.outline_line_start_arrow_notch_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(80.dp))
                Image(
                    painter = painterResource(R.drawable.outline_line_end_arrow_notch_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(24.dp)
                )
            }

            // C. El Layout de los Iconos (Capa de Presentación)
            CircularDialLayout(
                modifier = Modifier.fillMaxSize(),
                items = currentItems,
                currentRotation = rotationAngle.value,
                iconPathRadius = iconPathRadius,
                isAnimatingOut = isAnimatingOut,
                clickedIconIndex = clickedIconIndex,
                isExpansionFinished = isExpansionFinished
            )
        }
    }
}