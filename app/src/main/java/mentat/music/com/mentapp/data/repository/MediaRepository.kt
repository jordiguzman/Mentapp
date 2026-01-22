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
    // HELPER: Convertidor de JSON a Base de Datos (ARQUITECTURA CORRECTA)
    private fun mapToEntity(list: List<CarouselItem>?, category: String, targetList: MutableList<MediaEntity>) {
        if (list == null) return

        list.forEach { item ->

            // 1. LÓGICA DE CLASIFICACIÓN
            val esNoticia = category in listOf("Audio", "Blog", "Divulgacion")

            // 2. PREPARACIÓN DE DATOS
            // Si es noticia, el texto real viene en el JSON 'artist' -> Lo movemos a DB 'content'
            val textoParaGuardar = if (esNoticia) item.artist else null

            // Si es noticia, queremos la FECHA en el subtítulo. Si es música, el ARTISTA.
            val subtituloParaGuardar = if (esNoticia) item.date else item.artist

            // 3. DEBUG DE VERIFICACIÓN (Solo para el item problemático)
            if (item.title?.contains("plugins", ignoreCase = true) == true) {
                Log.d("MENTAPP_ARCH", "Guardando en DB -> Content: ${textoParaGuardar?.length ?: 0} chars | Artist: $subtituloParaGuardar")
            }

            targetList.add(
                MediaEntity(
                    category = category,
                    title = item.title,

                    // --- AQUÍ ESTÁ LA CORRECCIÓN ---
                    content = textoParaGuardar,      // El resumen va a su sitio
                    artist = subtituloParaGuardar,   // La fecha/artista va a su sitio
                    // -------------------------------

                    titleEn = item.titleEn,
                    contentEn = item.artistEn, // En inglés mantenemos la lógica similar

                    imageUrl = item.imageUrl,
                    targetUrl = item.targetUrl,
                    targetUrlEn = item.targetUrlEn,
                    appPackageName = item.appPackageName
                )
            )
        }
    }


}