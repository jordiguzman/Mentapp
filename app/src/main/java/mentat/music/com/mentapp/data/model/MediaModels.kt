package mentat.music.com.mentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CarouselItem(
    val id: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val category: String? = null,

    // --- MAPEO CLAVE ---
    // En el JSON nuevo, 'artist' trae el RESUMEN en Español
    val artist: String? = null,

    // En el JSON nuevo, 'content' viene vacío (pero lo definimos para que no falle)
    val content: String? = null,

    // --- CAMPOS NUEVOS (Coincidiendo con el JSON del Servidor) ---
    val titleEn: String? = null,      // Ojo: camelCase (sin guion bajo)
    val artistEn: String? = null,     // Aquí viene el Resumen en Inglés
    val contentEn: String? = null,    // Vacío
    val targetUrlEn: String? = null,  // URL Inglesa

    val date: String? = null,         // ¡Importante! La fecha de publicación

    val appPackageName: String? = null
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