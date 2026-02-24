package mentat.music.com.mentapp.ui.screens.splash

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun MeliesDialShutter(
    isAppExiting: Boolean,
    onExitAnimationComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val maxRadius = remember(screenWidth, screenHeight) {
        sqrt(screenWidth * screenWidth + screenHeight * screenHeight)
    }

    // Empezamos siempre cerrados a cal y canto (0f)
    val irisRadius = remember { Animatable(0f) }

    // --- LA MAGIA: DETECCIÓN DE FOCO ---
    val windowInfo = LocalWindowInfo.current
    var isWindowReady by remember { mutableStateOf(false) }

    // El sistema nos avisará cuando el Splash Screen desaparezca y la app sea visible
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (windowInfo.isWindowFocused && !isWindowReady) {
            // Un micro-respiro de 50ms (2 o 3 fotogramas) para asegurar el renderizado
            delay(50)
            isWindowReady = true
        }
    }

    // --- 1. APERTURA (Sincronizada con el sistema) ---
    LaunchedEffect(isWindowReady) {
        if (isWindowReady && !isAppExiting) {
            irisRadius.animateTo(
                targetValue = maxRadius,
                animationSpec = tween(
                    durationMillis = 2000, // Tiempo de apertura a tu gusto
                    easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
                )
            )
        }
    }

    // --- 2. CIERRE (La salida que ya funciona genial) ---
    LaunchedEffect(isAppExiting) {
        if (isAppExiting) {
            irisRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
            onExitAnimationComplete()
        }
    }

    val shutterBrush = remember(maxRadius) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF2B2B2B), Color(0xFF000000)),
            center = Offset(screenWidth / 2, screenHeight / 2),
            radius = maxRadius
        )
    }

    val borderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                Color.LightGray.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.9f)
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val currentRadius = irisRadius.value
                    if (currentRadius >= maxRadius * 0.99f) return@drawWithContent

                    val shutterPath = Path().apply {
                        addRect(Rect(0f, 0f, size.width, size.height))
                        val circlePath = Path().apply {
                            addOval(Rect(center = center, radius = currentRadius))
                        }
                        op(this, circlePath, PathOperation.Difference)
                    }

                    clipPath(shutterPath) {
                        drawRect(
                            brush = shutterBrush,
                            alpha = (1f - (currentRadius / maxRadius * 0.3f)).coerceIn(0f, 1f)
                        )
                    }

                    drawCircle(
                        brush = borderBrush,
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 6.dp.toPx()),
                        alpha = (1f - (currentRadius / maxRadius)).coerceIn(0f, 1f)
                    )
                }
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}