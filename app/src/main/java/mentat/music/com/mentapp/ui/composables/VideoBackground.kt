package mentat.music.com.mentapp.ui.composables

import android.content.Context
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
@Composable
fun VideoBackgroundDual(
    modifier: Modifier = Modifier,
    isBlueMode: Boolean
) {
    val context = LocalContext.current

    val videoUrlRosa = "https://mentat-music.com/mentapp_server/morado_loop.mp4"
    val videoUrlAzul = "https://mentat-music.com/mentapp_server/azul_loop.mp4"

    // 1. Configuración de Players
    val exoPlayerRosa = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrlRosa.toUri()))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    val exoPlayerAzul = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrlAzul.toUri()))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    // 2. Animación de Transparencia
    val alphaRosa by animateFloatAsState(
        targetValue = if (isBlueMode) 0f else 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "alphaRosa"
    )

    val alphaAzul by animateFloatAsState(
        targetValue = if (isBlueMode) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alphaAzul"
    )

    // 3. Limpieza de memoria al salir
    DisposableEffect(Unit) {
        onDispose {
            exoPlayerRosa.release()
            exoPlayerAzul.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // VIDEO ROSA
        AndroidView(
            // CAMBIO AQUÍ: Añadimos ": Context" para forzar el tipo
            factory = { ctx: Context ->
                TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                exoPlayerRosa.setVideoTextureView(view)
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaRosa)
        )

        // VIDEO AZUL
        AndroidView(
            // CAMBIO AQUÍ TAMBIÉN
            factory = { ctx: Context ->
                TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                exoPlayerAzul.setVideoTextureView(view)
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAzul)
        )
    }
}