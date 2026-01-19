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

// Colores definidos
val MentatRed = Color(0.8f, 0.0f, 0.3f, 1.0f)
val MentatBlue = Color(0.15f, 0.3f, 0.7f, 1.0f)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AttractorBackground(
    modifier: Modifier = Modifier,
    isFrozen: Boolean,
    frozenTime: Float,
    isBlueMode: Boolean
) {
    // --- SIN ANIMACIÓN: CORTE DIRECTO ---
    // Elegimos el color directamente según el modo. ¡PUM!
    val targetColor = if (isBlueMode) MentatBlue else MentatRed

    // --- Lógica del Shader ---
    val context = LocalContext.current
    val shaderString = remember {
        context.resources.openRawResource(R.raw.attractor_shader)
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
        ), label = "time"
    )

    Box(
        modifier = modifier
            .drawWithCache {
                val timeToRender = if (isFrozen) frozenTime else time

                shader.setFloatUniform("u_time", timeToRender)
                shader.setFloatUniform("u_resolution", size.width - 100f, size.height - 100f)

                // Inyectamos el color directo
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