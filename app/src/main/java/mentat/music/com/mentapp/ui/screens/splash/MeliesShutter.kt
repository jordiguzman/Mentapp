package mentat.music.com.mentapp.ui.screens.splash // O el paquete donde lo tengas

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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * EL OBTURADOR "DIAL" SOFISTICADO 🎛️
 * Efecto de apertura de iris con estilo de cristal/metal, bordes blancos y degradados.
 */
@Composable
fun MeliesDialShutter(
    content: @Composable () -> Unit
) {
    // CORRECCIÓN DEL ERROR DE CONTEXTO:
    // En lugar de castear "a lo bruto", usamos la función auxiliar findActivity()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // --- 1. GEOMETRÍA ---
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Calculamos la diagonal
    val maxRadius = remember(screenWidth, screenHeight) {
        sqrt(screenWidth * screenWidth + screenHeight * screenHeight)
    }

    // --- 2. ANIMACIÓN ---
    val irisRadius = remember { Animatable(0f) }
    var isExiting by remember { mutableStateOf(false) }

    // --- GRADIENTES ESTILO MENTAPP ---

    // El "Metal" del obturador (Gris muy oscuro)
    val shutterBrush = remember(maxRadius) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF2B2B2B), // Gris oscuro centro
                Color(0xFF000000)  // Negro puro esquinas
            ),
            center = Offset(screenWidth / 2, screenHeight / 2),
            radius = maxRadius
        )
    }

    // El "Bisel" (Borde brillante blanco/gris)
    val borderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                Color.LightGray.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.9f)
            )
        )
    }

    // --- 3. SECUENCIA DE APERTURA ---
    LaunchedEffect(Unit) {
        // PASO 1: Aseguramos que empieza CERRADO (Radio 0)
        irisRadius.snapTo(0f)

        // PASO 2: EL RETRASO TÁCTICO (La clave del problema)
        // Esperamos a que el Splash de Android termine su "show" y se quite.
        // Si lo pones muy corto, se solapa. Si lo pones muy largo, verás pantalla negra un rato.
        // 600ms - 800ms suele ser el punto dulce.
        delay(500)

        // PASO 3: AHORA SÍ, ACCIÓN 🎬
        irisRadius.animateTo(
            targetValue = maxRadius,
            animationSpec = tween(
                durationMillis = 4800, // Tu velocidad lenta majestuosa
                easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
            )
        )
    }

    // --- 4. SECUENCIA DE CIERRE ---
    // (Ahora mismo está desactivada la detección automática,
    // pero la lógica está lista para cuando la conectemos)
    LaunchedEffect(isExiting) {
        if (isExiting) {
            irisRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            )
            activity?.finish() // Ahora sí cierra la app de forma segura
        }
    }

    // --- 5. EL DIBUJO ---
    Box(modifier = Modifier.fillMaxSize()) {

        // A) LA APP
        content()

        // B) EL OBTURADOR
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val currentRadius = irisRadius.value

                    // Si ya está abierto del todo, no dibujamos para ahorrar batería
                    if (currentRadius >= maxRadius * 0.99f) return@drawWithContent

                    // Paso 1: Máscara Inversa (Rectángulo - Círculo)
                    val shutterPath = Path().apply {
                        addRect(Rect(0f, 0f, size.width, size.height))
                        val circlePath = Path().apply {
                            addOval(Rect(center = center, radius = currentRadius))
                        }
                        op(this, circlePath, PathOperation.Difference)
                    }

                    // Paso 2: Relleno oscuro
                    clipPath(shutterPath) {
                        drawRect(
                            brush = shutterBrush,
                            // Se hace sutilmente transparente al final
                            alpha = (1f - (currentRadius / maxRadius * 0.3f)).coerceIn(0f, 1f)
                        )
                    }

                    // Paso 3: Anillo Metálico
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

// --- FUNCIÓN AUXILIAR PARA EL CONTEXTO (La solución a tu error) ---
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity() // Pelamos la cebolla recursivamente
    else -> null
}