package mentat.music.com.mentapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import mentat.music.com.mentapp.R

object NavigationUtils {

    fun launchUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            // Flag necesario si lanzamos intent desde contexto no-Activity,
            // aunque desde Composable suele ir bien, esto previene crashes.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir enlace", Toast.LENGTH_SHORT).show()
        }
    }

    fun showComingSoon(context: Context) {
        Toast.makeText(
            context,
            context.getString(R.string.msg_coming_soon),
            Toast.LENGTH_SHORT
        ).show()
    }
}