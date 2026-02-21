package mentat.music.com.mentapp.ui.composables // O tu paquete correcto

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.ui.screens.home.DialConstants
import mentat.music.com.mentapp.ui.theme.VerdanaFontFamily
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun DialLayer(
    modifier: Modifier = Modifier,
    currentItems: List<DialItem>,
    isPortrait: Boolean,
    dialTitle: String,
    rotationAngle: Animatable<Float, AnimationVector1D>,
    dialScale: Animatable<Float, AnimationVector1D>,
    dialFlipX: Animatable<Float, AnimationVector1D>,
    dialBlur: Animatable<Float, AnimationVector1D>,
    isAnimatingOut: Boolean,
    clickedIconIndex: Int,
    isExpansionFinished: Boolean,
    isWebMenuOpen: Boolean,
    arrowsAlpha: Float,
    // NOTA: Eliminamos parámetros redundantes que ahora son constantes globales
    radiusPx: Float,
    thicknessPx: Float,
    onRotationComplete: (Float) -> Unit,
    scope: CoroutineScope
) {
    Box(
        modifier = modifier
            .pointerInput(clickedIconIndex, isAnimatingOut, isExpansionFinished, isWebMenuOpen) {
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
                        val currentOffset = rotationAngle.value - DialConstants.TARGET_ANGLE_RAD
                        val nearestIconIndex = -(currentOffset / DialConstants.ANGLE_STEP).roundToInt()
                        val targetSnapAngle = DialConstants.TARGET_ANGLE_RAD - (DialConstants.ANGLE_STEP * nearestIconIndex)

                        scope.launch {
                            rotationAngle.animateTo(
                                targetValue = targetSnapAngle,
                                animationSpec = spring(dampingRatio = DialConstants.SPRING_DAMPING, stiffness = 100f)
                            )
                            onRotationComplete(targetSnapAngle)
                        }
                    }
                )
            }
    ) {
        Text(
            text = dialTitle,
            color = Color.White.copy(0.5f),
            fontSize = 22.sp,
            fontFamily = VerdanaFontFamily,
            modifier = Modifier
                .align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart)
                .padding(32.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(dialScale.value)
                .graphicsLayer {
                    // 1. Aplicamos el giro de moneda
                    scaleX = dialScale.value * dialFlipX.value
                    scaleY = dialScale.value

                    // 2. Aplicamos el Motion Blur aquí mismo
                    // Si dialBlur.value es 60, el estiramiento será masivo
                    if (dialBlur.value > 0.5f) {
                        renderEffect = androidx.compose.ui.graphics.BlurEffect(
                            radiusX = dialBlur.value,
                            radiusY = 0.5f,
                            edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val brush = Brush.sweepGradient(DialConstants.DIAL_GRADIENT, center = center)
                drawCircle(brush, radiusPx, style = Stroke(thicknessPx))
                // Decoraciones sutiles (aros blancos)
                drawCircle(Color.White.copy(0.8f), radiusPx - (thicknessPx / 2), style = Stroke(1.5.dp.toPx()))
                drawCircle(Color.White.copy(0.5f), radiusPx + (thicknessPx / 2), style = Stroke(2.dp.toPx()))
            }

            Row(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = DialConstants.ICON_PATH_RADIUS)
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

            CircularDialLayout(
                modifier = Modifier.fillMaxSize(),
                items = currentItems,
                currentRotation = rotationAngle.value,
                iconPathRadius = DialConstants.ICON_PATH_RADIUS,
                isAnimatingOut = isAnimatingOut,
                clickedIconIndex = clickedIconIndex,
                isExpansionFinished = isExpansionFinished
            )
        }
    }
}