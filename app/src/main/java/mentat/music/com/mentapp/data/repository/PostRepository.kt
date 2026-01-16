package mentat.music.com.mentapp.data.repository

import kotlinx.coroutines.flow.Flow
import mentat.music.com.mentapp.data.PostEntity
import mentat.music.com.mentapp.data.local.PostDao
import mentat.music.com.mentapp.data.remote.RetrofitClient
import mentat.music.com.mentapp.utils.DateUtils
import java.util.regex.Pattern

class PostRepository(private val dao: PostDao) {

    private val api = RetrofitClient.api

    // Esta función devuelve los datos guardados en local para que la UI los vea siempre
    fun getPosts(language: String): Flow<List<PostEntity>> {
        return dao.getPostsByLanguage(language)
    }

    // Esta función va a internet, descarga y actualiza la base de datos
    suspend fun refreshPosts(language: String) {
        try {
            // 1. Decidir qué feed pedir
            val feedDto = if (language == "es") api.getFeedEs() else api.getFeedEn()

            // 2. Obtener la lista de items (o lista vacía si falla)
            val rssItems = feedDto.channel?.items ?: emptyList()

            // 3. Convertir (Mapear) DTO -> Entity
            val entities = rssItems.mapNotNull { dto ->
                // Si no hay link o título, saltamos la noticia (es basura)
                if (dto.link == null || dto.title == null) return@mapNotNull null

                // TRUCO: Extraer la imagen del HTML usando Regex
                val imageUrl = extractImageFromContent(dto.content ?: dto.description)

                PostEntity(
                    link = dto.link!!,
                    title = dto.title!!,
                    content = dto.content ?: "",
                    imageUrl = imageUrl,
                    pubDate = DateUtils.parseRssDate(dto.pubDate),
                    language = language
                )
            }

            // 4. Guardar en Base de Datos (Esto dispara la actualización de la UI automáticamente)
            if (entities.isNotEmpty()) {
                dao.insertPosts(entities)
                println("MENTAT_DEBUG: Se han guardado ${entities.size} noticias en idioma $language")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            println("MENTAT_ERROR: Fallo al descargar feed: ${e.message}")
            // Aquí podríamos manejar errores (avisar al usuario), por ahora solo log
        }
    }

    // Función auxiliar para buscar <img src="...">
    private fun extractImageFromContent(html: String?): String? {
        if (html.isNullOrEmpty()) return null
        // Busca: src="lo_que_sea"
        val matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(html)
        return if (matcher.find()) {
            matcher.group(1) // Devuelve la URL encontrada
        } else {
            null
        }
    }
}