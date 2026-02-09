package mentat.music.com.mentapp.ui.composables

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
import kotlin.math.roundToInt

@Composable
fun WebMenuLayer(
    modifier: Modifier = Modifier,
    // Estados
    isVisible: Boolean,
    rotationAnim: Animatable<Float, AnimationVector1D>,
    items: List<DialItem>,
    // Callbacks
    onClose: () -> Unit,
    onRotationChanged: (Float) -> Unit,
    // Utils
    scope: CoroutineScope,
    onVibrate: () -> Unit
) {
    // Animación de escala interna (Zoom in/out al abrir/cerrar)
    val miniDialScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "miniDialScale"
    )

    // Solo renderizamos si tiene un tamaño visible mínimo para ahorrar recursos
    if (isVisible || miniDialScale > 0.1f) {
        Box(
            modifier = modifier
                .offset(y = 140.dp) // Offset original de diseño
                .scale(miniDialScale),
            contentAlignment = Alignment.Center
        ) {
            // 1. Fondo del Dial (Anillo Gradiente Morado)
            Canvas(Modifier.size(245.dp)) {
                val brush = Brush.sweepGradient(
                    listOf(
                        Color.White.copy(0.95f),
                        Color.White.copy(0.2f),
                        Color.Gray.copy(0.5f),
                        Color.White.copy(0.2f),
                        Color.White.copy(0.95f)
                    ),
                    center = center
                )
                // Ancho del anillo (55dp) y radio (95dp)
                drawCircle(brush, radius = 95.dp.toPx(), style = Stroke(width = 55.dp.toPx()))
            }

            // 2. Lógica de Gestos y Contenido
            // Usamos CompositionLocalProvider para teñir los iconos de morado (#893471)
            CompositionLocalProvider(LocalContentColor provides Color(0xFF893471)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            val stepRad = (2 * Math.PI / 3).toFloat() // 120 grados por ítem
                            detectDragGestures(
                                onDragStart = {
                                    scope.launch { rotationAnim.stop() }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Factor de sensibilidad: dragAmount.x / 350
                                    scope.launch {
                                        rotationAnim.snapTo(rotationAnim.value + ((dragAmount.x / 350) * -1f))
                                    }
                                },
                                onDragEnd = {
                                    // Lógica de Snapping a 3 posiciones
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
                        radius = 95.dp
                    )
                }
            }

            // 3. Botón de Cerrar (X)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        onClose()
                        onVibrate()
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_web_foreground), // Asegúrate de que este recurso existe o usa Icons.Default.Close
                    contentDescription = "Cerrar menú web",
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}