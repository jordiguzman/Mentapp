package mentat.music.com.mentapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mentat.music.com.mentapp.data.PostEntity
import mentat.music.com.mentapp.data.local.dao.MediaDao
import mentat.music.com.mentapp.data.local.dao.PostDao
import mentat.music.com.mentapp.data.local.entity.MediaEntity

// CAMBIO IMPORTANTÍSIMO: Subimos a versión 3
@Database(entities = [PostEntity::class, MediaEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mentat_database"
                )
                    // Esto detectará que pasamos de v2 a v3 y reconstruirá las tablas nuevas
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}