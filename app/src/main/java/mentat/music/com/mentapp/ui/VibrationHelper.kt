package mentat.music.com.mentapp.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
// Asegúrate de importar donde hayas puesto el archivo UserPreferences
import mentat.music.com.mentapp.data.UserPreferences

/**
 * El "Especialista en Temblores".
 * Ahora es inteligente: consulta las preferencias del usuario antes de actuar.
 */
class VibrationHelper(private val context: Context) { // <--- AHORA ES 'private val'

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * EL "PUM": Golpe seco para clics.
     */
    fun vibrateClick() {
        // 1. EL FILTRO: Si el usuario dijo NO, no hacemos nada.
        if (!UserPreferences.isVibrationEnabled(context)) return

        // 2. LA ACCIÓN
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    }

    /**
     * EL "TICK": Golpe metálico para el dial.
     */
    fun vibrateTick() {
        // 1. EL FILTRO
        if (!UserPreferences.isVibrationEnabled(context)) return

        // 2. LA ACCIÓN
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    }
}

/**
 * Función "fábrica" para usarlo fácilmente en Compose.
 */
@Composable
fun rememberVibrator(): VibrationHelper {
    val context = LocalContext.current
    return remember(context) {
        VibrationHelper(context)
    }
}