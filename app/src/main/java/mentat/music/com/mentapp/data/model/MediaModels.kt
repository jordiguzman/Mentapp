package mentat.music.com.mentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CarouselItem(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val appPackageName: String? = null
)

@Serializable
data class AppData(
    val GUZZ: List<CarouselItem>? = null,
    val Spotify: List<CarouselItem>? = null,
    val Bandcamp: List<CarouselItem>? = null,
    val Soundcloud: List<CarouselItem>? = null,
    val YouTube: List<CarouselItem>? = null,
    val Concepto: List<CarouselItem>? = null
)