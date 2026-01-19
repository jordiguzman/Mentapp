package mentat.music.com.mentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mentat.music.com.mentapp.data.PostEntity

@Dao
interface PostDao {

    // 1. Insertar una lista de noticias (o una sola)
    // Si ya existe (mismo Link), la reemplaza.
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    // 2. Leer noticias filtradas por idioma
    // Las ordenamos por fecha (pubDate) descendente (las nuevas primero).
    // Devuelve un Flow para que la UI reaccione en tiempo real.
    @Query("SELECT * FROM posts WHERE language = :lang ORDER BY pubDate DESC")
    fun getPostsByLanguage(lang: String): Flow<List<PostEntity>>

    // 3. (Opcional) Borrar todo por si queremos limpiar caché antigua
    @Query("DELETE FROM posts WHERE language = :lang")
    suspend fun clearPostsByLanguage(lang: String)
}