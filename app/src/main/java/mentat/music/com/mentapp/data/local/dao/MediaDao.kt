package mentat.music.com.mentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mentat.music.com.mentapp.data.local.entity.MediaEntity

@Dao
interface MediaDao {
    // Inserta una lista de discos de golpe
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)

    // Borra todo (para limpiar antes de meter los datos nuevos)
    @Query("DELETE FROM media_items")
    suspend fun clearAll()

    // Nos da todos los items y la App ya los filtrará en memoria
    @Query("SELECT * FROM media_items")
    fun getAllMedia(): Flow<List<MediaEntity>>
}