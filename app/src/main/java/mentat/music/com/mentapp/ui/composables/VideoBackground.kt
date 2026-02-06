package mentat.music.com.mentapp.ui.composables

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoBackground(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val remoteVideoUrl = "https://mentat-music.com/mentapp_server/fondo_b_n.mp4"

    // 1. Creamos el EXOPLAYER (El cerebro)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(remoteVideoUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    // 2. Creamos la VISTA (PlayerView) que tiene superpoderes de escalado
    val playerView = remember {
        PlayerView(context).apply {
            player = exoPlayer
            useController = false // Ocultamos botones de pause/play

            // --- LA CLAVE DEL ÉXITO ---
            // RESIZE_MODE_ZOOM: "Haz zoom hasta llenar la pantalla, recorta lo que sobre"
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // 3. Ciclo de vida
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 4. Pintamos
    AndroidView(
        factory = { playerView },
        modifier = modifier // Asegúrate que aquí llega un .fillMaxSize()
    )
}