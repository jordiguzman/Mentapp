package mentat.music.com.mentapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // Asegúrate de tener esta anotación si usas Kotlin Serialization
data class CarouselItem(
    val id: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val category: String? = null,
    val artist: String? = null,
    val content: String? = null,

    // --- CORRECCIÓN: Los nombres deben coincidir con el JSON del servidor (CamelCase) ---
    @SerialName("titleEn") val titleEn: String? = null,         // Antes "title_en"
    @SerialName("artistEn") val artistEn: String? = null,       // Antes "artist_en"
    @SerialName("contentEn") val contentEn: String? = null,     // Antes "content_en"
    @SerialName("targetUrlEn") val targetUrlEn: String? = null, // Antes "targetUrl_en"

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