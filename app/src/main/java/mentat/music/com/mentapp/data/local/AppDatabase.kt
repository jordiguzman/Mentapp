package mentat.music.com.mentapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mentat.music.com.mentapp.data.PostEntity
import mentat.music.com.mentapp.data.local.dao.MediaDao
import mentat.music.com.mentapp.data.local.entity.MediaEntity

// 1. AÑADIMOS MediaEntity Y SUBIMOS VERSIÓN A 2
@Database(entities = [PostEntity::class, MediaEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun mediaDao(): MediaDao // <--- 2. AÑADIMOS ESTO

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
                    .fallbackToDestructiveMigration() // <--- ESTO BORRA LA BD VIEJA Y CREA LA NUEVA (VERSIÓN 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}