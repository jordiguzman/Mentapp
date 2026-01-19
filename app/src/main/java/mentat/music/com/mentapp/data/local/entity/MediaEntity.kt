package mentat.music.com.mentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,

    // --- CONTENIDO EN ESPAÑOL (Original) ---
    val title: String?,
    val content: String?, // <--- NUEVO: Resumen en español (nos faltaba)
    val artist: String?,  // (En noticias aquí irá la FECHA)
    val imageUrl: String?,
    val targetUrl: String?,

    // --- CONTENIDO EN INGLÉS (Nuevos campos) ---
    val titleEn: String?,     // <--- Título en Inglés
    val contentEn: String?,   // <--- Resumen en Inglés
    val targetUrlEn: String?, // <--- Link en Inglés

    // --- SISTEMA ---
    val appPackageName: String?
)