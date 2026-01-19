package mentat.music.com.mentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CarouselItem(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val title: String? = null,
    val content: String? = null, // <--- NUEVO: Resumen en Español
    val artist: String? = null,
    val appPackageName: String? = null,

    // --- NUEVOS CAMPOS EN INGLÉS (Deben llamarse igual que en el JSON) ---
    val title_en: String? = null,
    val content_en: String? = null,
    val targetUrl_en: String? = null
)

@Serializable
data class AppData(
    // --- MÚSICA (Viene de mentat_data_DEF.json) ---
    val GUZZ: List<CarouselItem>? = null,
    val Spotify: List<CarouselItem>? = null,
    val Bandcamp: List<CarouselItem>? = null,
    val Soundcloud: List<CarouselItem>? = null,
    val YouTube: List<CarouselItem>? = null,

    // --- NOTICIAS (Viene de app_data.json - WordPress) ---
    val Audio: List<CarouselItem>? = null,       // Antes Tutoriales
    val Divulgacion: List<CarouselItem>? = null, // Antes Ciencia
    val Blog: List<CarouselItem>? = null,

    // --- Legacy / Otros ---
    val Concepto: List<CarouselItem>? = null
)