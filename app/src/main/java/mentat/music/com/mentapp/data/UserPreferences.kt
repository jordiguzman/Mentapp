package mentat.music.com.mentapp.data // O tu paquete correspondiente

import android.content.Context
import androidx.core.content.edit

object UserPreferences {
    private const val PREFS_NAME = "mentat_app_prefs"
    private const val KEY_VIBRATION = "vibration_enabled"

    // Por defecto devuelve TRUE (vibración activada)
    fun isVibrationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_VIBRATION, enabled) }
    }
}