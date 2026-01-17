package mentat.music.com.mentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // AQUÍ guardaremos "Spotify", "Bandcamp", etc.
    val title: String?,
    val artist: String?,
    val imageUrl: String?,
    val targetUrl: String?,
    val appPackageName: String?
)