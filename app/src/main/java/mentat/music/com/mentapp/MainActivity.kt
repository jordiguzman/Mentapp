package mentat.music.com.mentapp

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // ¡Para que la app ocupe toda la pantalla!
import androidx.annotation.RequiresApi
import mentat.music.com.mentapp.ui.navigation.AppNavigation
import mentat.music.com.mentapp.ui.theme.MentappTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // <-- ¡Añade esta!
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.ui.screens.splash.MeliesDialShutter

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. CAMBIO: Guardamos la instancia del Splash en una variable
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. AÑADIDO: La lógica de bloqueo
        var isSplashVisible = true

        // Le decimos al Splash: "No te vayas hasta que esta variable sea false"
        splashScreen.setKeepOnScreenCondition {
            isSplashVisible
        }

        // Lanzamos un temporizador de 2 segundos (o lo que quieras)
        lifecycleScope.launch {
            delay(2000) // Tiempo en milisegundos
            isSplashVisible = false // ¡Libera al Kraken! (Cierra el splash)
        }

        // El resto sigue igual...
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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