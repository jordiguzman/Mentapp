package mentat.music.com.mentapp.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import mentat.music.com.mentapp.R

// Definición pura de datos (sin lambdas ni lógica)
data class MenuOption(
    val id: String,
    val label: String,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val color: Color
)

object HomeMenuConfig {
    val dial1Options = listOf(
        MenuOption("Bluesky", "Bluesky", iconRes = R.drawable.ic_menu_social, color = Color(0xFF0085FF)),
        MenuOption("YouTube", "YouTube", iconRes = R.drawable.ic_menu_youtube, color = Color(0xFFFF0000)),
        MenuOption("Spotify", "Spotify", iconRes = R.drawable.ic_menu_streams, color = Color(0xFF1DB954)),
        MenuOption("Bandcamp", "Bandcamp", iconRes = R.drawable.ic_menu_bandcamp, color = Color(0xFF629AA9)),
        MenuOption("SoundCloud", "SoundCloud", iconRes = R.drawable.ic_menu_soundcloud, color = Color(0xFFFF5500)),
        MenuOption("Web", "Mundo Web", iconRes = R.drawable.ic_logo_mentat, color = Color(0xFF893471))
    )

    val dial2Options = listOf(
        MenuOption("GUZZ", "GUZZ", iconRes = R.drawable.ic_menu_guzz, color = Color.White),
        MenuOption("DJSessions", "DJ Sessions", iconRes = R.drawable.ic_sessions, color = Color(0xFF000000)),
        MenuOption("Subs", "Suscriptores", iconVector = Icons.Default.Lock, color = Color(0xFF000000)),
        MenuOption("Archive", "Archivo", iconRes = R.drawable.ic_menu_concept, color = Color(0xFF8A2BE2)),
        MenuOption("Contact", "Contacto", iconRes = R.drawable.ic_mail, color = Color(0xFF000000)),
        MenuOption("Live", "Directo", iconRes = R.drawable.ic_live_music, color = Color(0xFF000000))
    )

    val webMenuOptions = listOf(
        MenuOption("Audio", "Tutoriales", iconRes = R.drawable.ic_audio, color = Color(0xFF000000)),
        MenuOption("Divulgacion", "Ciencia", iconRes = R.drawable.ic_divulgacion, color = Color(0xFF000000)),
        MenuOption("Blog", "Blog", iconRes = R.drawable.ic_blog, color = Color(0xFF000000))
    )
}