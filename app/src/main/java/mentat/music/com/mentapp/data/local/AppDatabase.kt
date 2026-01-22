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
@Database(entities = [MediaEntity::class], version = 3, exportSchema = false) // <--- CAMBIO 1: Versión 2
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_database" // El nombre de tu DB
                )
                    .fallbackToDestructiveMigration() // <--- CAMBIO 2: OBLIGATORIO
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}