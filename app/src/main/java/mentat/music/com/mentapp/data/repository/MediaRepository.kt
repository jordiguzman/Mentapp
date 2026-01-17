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

    private val JSON_URL = "https://mentat-music.com/mentapp/mentat_data_DEF.json"

    // Cliente HTTP (Igual que tenías en el ViewModel)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    // 1. LEER DATOS (OBSERVAR)
    // Devuelve todos los datos de la BD. La UI filtrará lo que necesite.
    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    // 2. ACTUALIZAR DATOS (DESCARGAR Y GUARDAR)
    suspend fun refreshMedia() {
        try {
            // A) Truco anti-caché para descargar siempre el archivo fresco
            val uniqueUrl = "$JSON_URL?t=${System.currentTimeMillis()}"
            val appData = client.get(uniqueUrl).body<AppData>()

            // B) Convertimos el árbol JSON a una lista plana de Entities
            val masterList = mutableListOf<MediaEntity>()

            // Función auxiliar para mapear
            fun mapToEntity(list: List<CarouselItem>?, category: String) {
                list?.forEach { item ->
                    masterList.add(
                        MediaEntity(
                            category = category,
                            title = item.title,
                            artist = item.artist,
                            imageUrl = item.imageUrl,
                            targetUrl = item.targetUrl,
                            appPackageName = item.appPackageName
                        )
                    )
                }
            }

            // Mapeamos cada sección
            mapToEntity(appData.GUZZ, "GUZZ")
            mapToEntity(appData.Spotify, "Spotify")
            mapToEntity(appData.Bandcamp, "Bandcamp")
            mapToEntity(appData.Soundcloud, "Soundcloud")
            mapToEntity(appData.YouTube, "YouTube")
            // Concepto/Entradas lo ignoramos porque va por RSS,
            // pero si quisieras guardarlo también, añádelo aquí.

            // C) Transacción de Base de Datos: Borrar viejo -> Meter nuevo
            if (masterList.isNotEmpty()) {
                mediaDao.clearAll()
                mediaDao.insertAll(masterList)
                Log.d("MENTAT_REPO", "JSON actualizado: ${masterList.size} items guardados.")
            }

        } catch (e: Exception) {
            Log.e("MediaRepository", "Error actualizando JSON", e)
            // No hacemos nada más: si falla, la app seguirá mostrando los datos viejos de Room
        }
    }
}