package mentat.music.com.mentapp.ui.composables // O tu paquete

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.ui.screens.home.DialConstants
import kotlin.math.roundToInt

@Composable
fun WebMenuLayer(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    rotationAnim: Animatable<Float, AnimationVector1D>,
    items: List<DialItem>,
    onClose: () -> Unit,
    onRotationChanged: (Float) -> Unit,
    scope: CoroutineScope,
    onVibrate: () -> Unit
) {
    val miniDialScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "miniDialScale"
    )

    if (isVisible || miniDialScale > 0.1f) {
        Box(
            modifier = modifier
                .offset(y = DialConstants.WEB_MENU_OFFSET_Y)
                .scale(miniDialScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(245.dp)) {
                val brush = Brush.sweepGradient(DialConstants.WEB_MENU_GRADIENT, center = center)
                drawCircle(
                    brush,
                    radius = DialConstants.WEB_MENU_RADIUS.toPx(),
                    style = Stroke(width = DialConstants.WEB_MENU_STROKE.toPx())
                )
            }

            CompositionLocalProvider(LocalContentColor provides DialConstants.COLOR_WEB_MENU) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            val stepRad = (2 * Math.PI / 3).toFloat()
                            detectDragGestures(
                                onDragStart = { scope.launch { rotationAnim.stop() } },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        rotationAnim.snapTo(rotationAnim.value + ((dragAmount.x / 350) * -1f))
                                    }
                                },
                                onDragEnd = {
                                    val steps = (rotationAnim.value / stepRad).roundToInt()
                                    val target = steps * stepRad
                                    scope.launch {
                                        rotationAnim.animateTo(target, spring(0.6f, 300f))
                                        onRotationChanged(target)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MiniCircularDialLayout(
                        modifier = Modifier.fillMaxSize(),
                        items = items,
                        currentRotation = rotationAnim.value,
                        radius = DialConstants.WEB_MENU_RADIUS
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onClose(); onVibrate() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_web_foreground),
                    contentDescription = "Cerrar",
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}