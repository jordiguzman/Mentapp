package mentat.music.com.mentapp.utils

import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {
    // Formato estándar de RSS: "Mon, 15 Jan 2024 10:00:00 +0000"
    private val rssFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

    fun parseRssDate(dateString: String?): Long {
        if (dateString == null) return System.currentTimeMillis()
        return try {
            rssFormat.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}