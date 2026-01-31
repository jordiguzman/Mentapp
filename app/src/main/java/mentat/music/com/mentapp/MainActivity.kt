package mentat.music.com.mentapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // ¡Para que la app ocupe toda la pantalla!
import androidx.annotation.RequiresApi
import mentat.music.com.mentapp.ui.navigation.AppNavigation
import mentat.music.com.mentapp.ui.theme.MentappTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // <-- ¡Añade esta!
import mentat.music.com.mentapp.ui.screens.splash.MeliesDialShutter

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // ¡Importante para el fondo inmersivo!
        setContent {
            MentappTheme {
                // 2. AQUI LLAMAMOS AL OBTURADOR
                // Envuelve a toda la navegación para pintar por encima
                MeliesDialShutter {
                    AppNavigation()
                }
            }
        }
    }
}