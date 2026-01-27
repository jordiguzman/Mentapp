package mentat.music.com.mentapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // <--- ESTO ES VITAL PARA KTOR
data class CarouselItem(
    val id: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val category: String? = null,
    val artist: String? = null,
    val content: String? = null,

    // --- MAPEO PARA KTOR / KOTLIN SERIALIZATION ---
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("artist_en") val artistEn: String? = null,
    @SerialName("content_en") val contentEn: String? = null,
    @SerialName("targetUrl_en") val targetUrlEn: String? = null,

    val date: String? = null,
    val appPackageName: String? = null
)

@Serializable
data class AppData(
    // --- ESTÁTICO (Música) ---
    val GUZZ: List<CarouselItem>? = null,
    val Spotify: List<CarouselItem>? = null,
    val Bandcamp: List<CarouselItem>? = null,
    val Soundcloud: List<CarouselItem>? = null,
    val YouTube: List<CarouselItem>? = null,

    // --- DINÁMICO (Noticias/WordPress) ---
    val Audio: List<CarouselItem>? = null,
    val Divulgacion: List<CarouselItem>? = null, // Cuidado: En el JSON debe venir sin tilde "Divulgacion"
    val Blog: List<CarouselItem>? = null
)