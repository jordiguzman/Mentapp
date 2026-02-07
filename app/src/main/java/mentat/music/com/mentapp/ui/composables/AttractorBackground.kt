package mentat.music.com.mentapp.ui.composables

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import mentat.music.com.mentapp.R
import kotlin.math.cos
import kotlin.math.sin

// Colores
val MentatRed = Color(0.8f, 0.0f, 0.3f, 1.0f)
val MentatBlue = Color(0.15f, 0.3f, 0.7f, 1.0f)

// Constantes del shader (movidas desde GLSL a Kotlin)
private const val A_BASE = -1.4f
private const val B_BASE = 1.6f
private const val C_BASE = 1.0f
private const val D_BASE = 0.7f
private const val A_SPEED = 0.05f
private const val B_SPEED = 0.03f
private const val C_SPEED = 0.1f
private const val D_SPEED = 0.07f

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AttractorBackground(
    modifier: Modifier = Modifier,
    isFrozen: Boolean,
    frozenTime: Float,
    isBlueMode: Boolean
) {
    // Cambio de color de golpe (sin animación)
    val targetColor = if (isBlueMode) MentatBlue else MentatRed

    val context = LocalContext.current
    val shaderString = remember {
        context.resources.openRawResource(R.raw.attractor_shader_claude)
            .bufferedReader()
            .use { it.readText() }
    }
    val shader = remember { RuntimeShader(shaderString) }
    val brush = remember { ShaderBrush(shader) }

    val infiniteTransition = rememberInfiniteTransition(label = "shader time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 600000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "time"
    )

    Box(
        modifier = modifier.drawWithCache {
            val timeToRender = if (isFrozen) frozenTime else time

            // MEJORA 1: Calcular parámetros una sola vez por frame (en CPU)
            val a = A_BASE + sin(timeToRender * A_SPEED) * 0.5f
            val b = B_BASE + cos(timeToRender * B_SPEED) * 0.4f
            val c = C_BASE + sin(timeToRender * C_SPEED) * 0.2f
            val d = D_BASE + cos(timeToRender * D_SPEED) * 0.3f

            shader.setFloatUniform("u_time", timeToRender)
            shader.setFloatUniform("u_resolution", size.width, size.height)
            shader.setFloatUniform("u_params", a, b, c, d) // ← Pasar los 4 valores
            shader.setFloatUniform(
                "u_color",
                targetColor.red,
                targetColor.green,
                targetColor.blue
            )

            onDrawBehind {
                drawRect(brush)
            }
        }
    )
}