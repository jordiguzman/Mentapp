package mentat.music.com.mentapp.data.repository

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import mentat.music.com.mentapp.data.local.dao.MediaDao
import mentat.music.com.mentapp.data.local.entity.MediaEntity
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem

class MediaRepository(private val mediaDao: MediaDao) {

    // DOS FUENTES DE DATOS:
    private val STATIC_URL = "https://mentat-music.com/mentapp_server/mentat_data_DEF.json"
    private val DYNAMIC_URL = "https://www.mentat-music.com/mentapp_server/app_data.json"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Vital: Ignora lo que no entienda de cada archivo
                coerceInputValues = true
            })
        }
    }

    // 1. LEER (La UI observa esto)
    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    // 2. ACTUALIZAR (La Gran Fusión)
    suspend fun refreshMedia() {
        try {
            val masterList = mutableListOf<MediaEntity>()
            val timestamp = System.currentTimeMillis() // Truco anti-caché

            // --- A) DESCARGAR MÚSICA (Estático) ---
            try {
                val staticData = client.get("$STATIC_URL?t=$timestamp").body<AppData>()

                // Mapeamos las secciones de música
                mapToEntity(staticData.Spotify, "Spotify", masterList)
                mapToEntity(staticData.Bandcamp, "Bandcamp", masterList)
                mapToEntity(staticData.Soundcloud, "Soundcloud", masterList)
                mapToEntity(staticData.GUZZ, "GUZZ", masterList)
                mapToEntity(staticData.YouTube, "YouTube", masterList)

                Log.d("REPO", "Música cargada correctamente")
            } catch (e: Exception) {
                Log.e("REPO", "Fallo al cargar música", e)
            }

            // --- B) DESCARGAR NOTICIAS (Dinámico - WordPress) ---
            try {
                val dynamicData = client.get("$DYNAMIC_URL?t=$timestamp").body<AppData>()

                // Mapeamos las secciones de noticias
                mapToEntity(dynamicData.Audio, "Audio", masterList)
                mapToEntity(dynamicData.Divulgacion, "Divulgacion", masterList) // Ojo: Clave JSON sin tilde
                mapToEntity(dynamicData.Blog, "Blog", masterList)

                Log.d("REPO", "Noticias cargadas correctamente")
            } catch (e: Exception) {
                Log.e("REPO", "Fallo al cargar noticias", e)
            }

            // --- C) GUARDAR TODO EN BASE DE DATOS ---
            if (masterList.isNotEmpty()) {
                mediaDao.clearAll()
                mediaDao.insertAll(masterList)
                Log.d("REPO", "FUSIÓN COMPLETADA: ${masterList.size} items guardados.")
            }

        } catch (e: Exception) {
            Log.e("MediaRepository", "Error general en refreshMedia", e)
        }
    }

    // HELPER: Convertidor de JSON a Base de Datos
    private fun mapToEntity(list: List<CarouselItem>?, category: String, targetList: MutableList<MediaEntity>) {
        list?.forEach { item ->
            targetList.add(
                MediaEntity(
                    category = category,

                    // Español
                    title = item.title,

                    // CAMBIO 1: El contenido visual ahora coge el resumen (que venía en 'artist' en el JSON)
                    content = item.artist,

                    // CAMBIO 2: Donde la App espera la fecha (campo 'artist' de la DB), metemos la fecha del JSON
                    artist = item.date,

                    imageUrl = item.imageUrl,
                    targetUrl = item.targetUrl,

                    // Inglés
                    titleEn = item.titleEn, // (Asegúrate que en CarouselItem se llame titleEn o title_en)

                    // CAMBIO 3: Lo mismo para el inglés
                    contentEn = item.artistEn,

                    targetUrlEn = item.targetUrlEn,

                    // Sistema
                    appPackageName = item.appPackageName
                )
            )
        }
    }
}