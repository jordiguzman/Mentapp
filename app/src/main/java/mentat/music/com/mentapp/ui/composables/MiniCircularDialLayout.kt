package mentat.music.com.mentapp.ui.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    // --- 1. COPIA EXACTA DE LA ANIMACIÓN DE PULSO DEL DIAL GRANDE ---
    val infiniteTransition = rememberInfiniteTransition(label = "mini_dial_pulse")

    // Escala: de 1.0 a 1.25 (latido)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseScale"
    )

    // Alpha: de 0.6 a 0.3 (desvanecimiento del latido)
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseAlpha"
    )

    Layout(
        modifier = modifier,
        content = {
            val angleStep = if (items.isNotEmpty()) (2 * PI / items.size).toFloat() else 0f
            val logicOffset = (targetAngleRad - visualShift) - currentRotation
            val rawIndex = (logicOffset / angleStep).roundToInt()

            // Calculamos quién está activo
            val activeIndex = if (items.isNotEmpty()) {
                var index = rawIndex % items.size
                if (index < 0) index += items.size
                index
            } else {
                -1
            }

            items.forEachIndexed { index, item ->
                val isActive = (index == activeIndex)

                // --- 2. ANIMACIÓN DE TAMAÑO (Spring) ---
                // Activo: 60dp, Inactivo: 40dp
                val targetSize = if (isActive) 60.dp else 40.dp
                val animatedSize by animateDpAsState(
                    targetValue = targetSize,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "size"
                )

                // --- 3. ANIMACIÓN DE OPACIDAD (Alpha) ---
                // El activo se ve al 100%, los otros un poco transparentes
                val targetAlpha = if (isActive) 1f else 0.6f
                val animatedAlpha by animateFloatAsState(targetAlpha, label = "alpha")

                val painter: Painter = when (item.icon) {
                    is Int -> painterResource(id = item.icon)
                    is ImageVector -> rememberVectorPainter(item.icon)
                    else -> painterResource(id = android.R.drawable.ic_menu_help)
                }

                // CONTENEDOR PRINCIPAL
                Box(
                    modifier = Modifier
                        .size(animatedSize)
                        .alpha(animatedAlpha)
                        .clickable { item.onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    // A. EL HALO DE COLOR (SOLO SI ESTÁ ACTIVO)
                    // Aquí aplicamos el "pulseScale" y "pulseAlpha" copiados del grande.
                    // Usamos el color MORADO (#893471) fijo.
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize() // Llena los 60dp
                                .scale(pulseScale) // LATIDO AQUÍ
                                .clip(CircleShape)
                                .background(
                                    color = Color(0xFF893471).copy(alpha = pulseAlpha) // MORADO + LATIDO ALPHA
                                )
                        )
                    }

                    // B. EL ICONO (SIEMPRE NEGRO)
                    // No cambia de color, solo de tamaño junto con la caja padre.
                    Icon(
                        painter = painter,
                        contentDescription = item.label,
                        tint = Color.Black, // SIEMPRE NEGRO
                        modifier = Modifier.size(animatedSize * 0.6f) // 60% del tamaño del contenedor
                    )
                }
            }
        }
    ) { measurables, constraints ->
        // Bloque de Layout matemático (Idéntico al original)
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