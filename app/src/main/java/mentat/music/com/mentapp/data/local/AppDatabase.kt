package mentat.music.com.mentapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mentat.music.com.mentapp.data.PostEntity

@Database(entities = [PostEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Aquí exponemos el DAO para que el resto de la app lo use
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Si ya existe, la devolvemos. Si no, la creamos.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mentat_database" // Nombre del archivo .db
                )
                    .fallbackToDestructiveMigration() // Si cambiamos la estructura, borra y crea de nuevo (útil en dev)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}