package mentat.music.com.mentapp.ui.composables

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mentat.music.com.mentapp.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// --- (data class MenuItem y menuItems - sin cambios) ---
data class MenuItem(
    val name: String,
    @DrawableRes val iconResId: Int,
    val route: String,
    val brandColor: Color
)
val menuItems = listOf(
    MenuItem("GUZZ", R.drawable.ic_menu_guzz, "guzz_screen", Color.White),
    MenuItem("Spotify", R.drawable.ic_menu_streams, "spotify_screen", Color(0xFF1DB954)),
    MenuItem("Social", R.drawable.ic_menu_social, "https://bsky.app/profile/juanmentat.bsky.social", Color(0xFF0085FF)),
    MenuItem("YouTube", R.drawable.ic_menu_youtube, "youtube_screen", Color(0xFFFF0000)),
    MenuItem("Entradas", R.drawable.ic_menu_concept, "https://www.mentat-music.com/site/concepto/", Color(0xFF8A2BE2)),
    MenuItem("Bandcamp", R.drawable.ic_menu_bandcamp, "bandcamp_screen", Color(0xFF629AA9)),
    MenuItem("Soundcloud", R.drawable.ic_menu_soundcloud, "soundcloud_screen", Color(0xFFFF5500))
)
val angleStep = (2 * Math.PI.toFloat() / menuItems.size)
val targetAngleRad = (Math.PI.toFloat() / 2.0f)


@Composable
fun CircularDialLayout(
    modifier: Modifier = Modifier,
    currentRotation: Float,
    iconPathRadius: Dp,
    isAnimatingOut: Boolean,
    clickedIconIndex: Int,
    isExpansionFinished: Boolean,
    onIconClick: (route: String, index: Int) -> Unit,
    contentFor: @Composable (
        item: MenuItem,
        isClickedIcon: Boolean,
        isExpansionFinished: Boolean,
        isActive: Boolean
    ) -> Unit
) {
    val radiusPx = with(LocalDensity.current) { iconPathRadius.toPx() }

    Layout(
        modifier = modifier,
        content = {
            menuItems.forEachIndexed { index, item ->

                // --- LÓGICA DE ÁNGULO Y ESTADO ---
                val angle = (angleStep * index) + currentRotation
                val normalizedAngle = (angle % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val targetAngleNorm = (targetAngleRad % (2 * Math.PI.toFloat()) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val diff = abs(normalizedAngle - targetAngleNorm)
                val isActive = (diff < 0.05f || abs(diff - 2 * Math.PI.toFloat()) < 0.05f)
                val isClickedIcon = (index == clickedIconIndex)

                // --- TAMAÑOS ---
                // 1. Tamaño del Icono Real
                val targetIconSize = when {
                    isAnimatingOut && isClickedIcon -> 1000.dp
                    isAnimatingOut && !isClickedIcon -> 48.dp
                    isActive -> 64.dp
                    else -> 48.dp
                }
                val animatedIconSize by animateDpAsState(
                    targetValue = targetIconSize,
                    animationSpec = tween(durationMillis = TRANSITION_DURATION),
                    label = "iconSizeAnimation"
                )

                // ==========================================================
                // --- AJUSTE FINO AQUÍ ---
                // ==========================================================

                // 1. ESPACIO EXTRA: Aquí defines cuánto más grande es la caja invisible.
                // Cuanto más grande, menos se recorta la sombra.
                // (64dp de icono + 40dp extra = 104dp de caja total)
                val extraSpaceForShadow = 80.dp

                // 2. ESCALA DE LA SOMBRA: Hacemos la sombra un poco más pequeña (0.85f = 85%)
                // Esto ayuda a esconder los bordes cuadrados duros del PNG detrás del icono real.
                val shadowScale = 0.85f

                val containerSize = animatedIconSize + extraSpaceForShadow

                // --- ANIMACIÓN DE APARICIÓN ---
                val containerTargetAlpha = when {
                    isAnimatingOut && !isClickedIcon -> 0.0f
                    else -> 1.0f
                }
                val containerAnimatedAlpha by animateFloatAsState(
                    targetValue = containerTargetAlpha,
                    animationSpec = tween(durationMillis = TRANSITION_DURATION),
                    label = "containerAlpha"
                )

                // --- CAJA PRINCIPAL (EL CONTENEDOR GRANDE) ---
                Box(
                    modifier = Modifier
                        .alpha(containerAnimatedAlpha)
                        .size(containerSize) // Usamos el tamaño con extra de espacio
                        .then(if (isActive && !isAnimatingOut) Modifier.clickable {
                            onIconClick(item.route, index)
                        } else Modifier),
                    contentAlignment = Alignment.Center // Todo centrado
                ) {

                    // ==========================================================
                    // --- SOMBRA AJUSTADA ---
                    // ==========================================================
                    if (!isAnimatingOut) {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.5f), // Color de la sombra
                            modifier = Modifier
                                .size(animatedIconSize) // Tamaño base
                                .scale(shadowScale)     // <--- TRUCO: La hacemos un pelín más pequeña
                                .offset(x = 6.dp, y = 6.dp) // Desplazamiento
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.blur(4.dp) // Difuminado
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }

                    // --- EL ICONO REAL (CONTENIDO) ---
                    // IMPORTANTE: Este Box tiene el tamaño exacto del icono original
                    // para que 'contentFor' no se pierda.
                    Box(
                        modifier = Modifier.size(animatedIconSize)
                    ) {
                        contentFor(
                            item,
                            isClickedIcon,
                            isExpansionFinished,
                            isActive
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        // --- LÓGICA DE LAYOUT (Sin cambios) ---
        val placables = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val centerX = layoutWidth / 2
        val centerY = layoutHeight / 2

        layout(layoutWidth, layoutHeight) {
            placables.forEachIndexed { index, placable ->
                val angle = (angleStep * index) + currentRotation
                val x = (centerX + radiusPx * cos(angle.toDouble())).toInt() - (placable.width / 2)
                val y = (centerY + radiusPx * sin(angle.toDouble())).toInt() - (placable.height / 2)
                placable.placeRelative(x, y)
            }
        }
    }
}