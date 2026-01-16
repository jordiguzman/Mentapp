package mentat.music.com.mentapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    // Usamos el enlace como ID único. Así, si bajas el feed de nuevo
    // y la URL es la misma, Room sabrá que es el mismo artículo.
    @PrimaryKey
    val link: String,

    val title: String,

    // El contenido corto (description) o el completo (content:encoded)
    // Guardaremos el completo para poder mostrarlo offline si queremos.
    val content: String,

    // La imagen destacada. Puede ser null si el post no tiene.
    val imageUrl: String?,

    // Fecha de publicación.
    // TRUCO: Aunque el RSS la da en texto ("Mon, 15 Jan..."),
    // aquí la guardaremos como Long (milisegundos) para poder ordenar
    // la lista cronológicamente de forma fácil.
    val pubDate: Long,

    // Importante: "es" o "en".
    // Así sabremos qué noticias mostrar según el botón del usuario.
    val language: String,

    // Un campo extra por si queremos marcar noticias como leídas en el futuro
    val isRead: Boolean = false
)