package com.dogumgunu.asistanim.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Yerel veritabanı. Veriler yalnızca cihazda saklanır.
 * Şema değişirse (version artarsa) veri kaybını önlemek için mutlaka Migration yazılmalıdır;
 * bilerek fallbackToDestructiveMigration kullanılmamıştır.
 */
@Database(entities = [Person::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dogum_gunu.db"
                ).build().also { instance = it }
            }
    }
}
