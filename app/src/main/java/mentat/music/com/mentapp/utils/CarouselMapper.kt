package mentat.music.com.mentapp.utils

import android.text.Html
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem

object CarouselMapper {

    fun mapToCarouselItems(
        itemId: String?,
        appData: AppData?,
        newsPosts: List<CarouselItem> // Usamos tu clase CarouselItem aquí también
    ): List<CarouselItem>? {

        if (itemId == null) return null

        // 1. Datos estáticos (Música) desde AppData
        if (appData != null) {
            when (itemId) {
                "GUZZ" -> return appData.GUZZ
                "Spotify" -> return appData.Spotify
                "Bandcamp" -> return appData.Bandcamp
                "SoundCloud" -> return appData.Soundcloud
                "YouTube" -> return appData.YouTube?.map {
                    // Truco para la imagen de YouTube
                    it.copy(imageUrl = "https://img.youtube.com/vi/${it.imageUrl}/0.jpg")
                }
            }
        }

        // 2. Datos dinámicos (Blog/Noticias)
        // Aquí limpiamos el HTML para que se vea bien en la tarjeta
        if (itemId in listOf("Audio", "Divulgacion", "Blog")) {
            return newsPosts.map { entity ->
                val rawContent = entity.content ?: ""

                // Limpieza de HTML (quita etiquetas <p>, <br>, etc.)
                val plainText = Html.fromHtml(
                    rawContent,
                    Html.FROM_HTML_MODE_LEGACY
                ).toString()
                    .replace("\uFFFC", "")
                    .replace("\n", " ")
                    .trim()

                // Devolvemos un CarouselItem nuevo con el texto limpio en 'artist'
                // (O en 'content', según donde lo muestre tu AlbumCarouselBox)
                CarouselItem(
                    title = entity.title,
                    imageUrl = entity.imageUrl,
                    targetUrl = entity.targetUrl,
                    artist = plainText // Ponemos el texto limpio aquí
                )
            }
        }

        return null
    }
}