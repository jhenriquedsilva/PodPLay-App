package com.raywenderlich.podplay.db

import android.content.Context
import androidx.room.*
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import kotlinx.coroutines.CoroutineScope
import java.util.*

// TypeConverters are methods
// The class is only a holder
// Since I removed the date parsing, I commented this code
/*
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}
*/

@Database(entities = [Podcast::class, Episode::class], version = 1)
// @TypeConverters(Converters::class)
abstract class PodPlayDatabase: RoomDatabase() {

    abstract fun podcastDao(): PodcastDao

    companion object {

        /**
         * @Volatile should be used on fields
         * Volatile fields provide memory visibility and guarantee that the value that is
         * being read, comes from the main memory and not the cpu-cache, so the value in cpu-cache
         * is always considered to be dirty, and It has to be fetched again
         */
        // I am not sure if this @Volatile is really necessary
        @Volatile
        private var INSTANCE: PodPlayDatabase? = null
                                        // I still do not know what is of this coroutine scope
        fun getInstance(context: Context, coroutineScope: CoroutineScope): PodPlayDatabase {

            val temporaryInstance = INSTANCE

            if (temporaryInstance!= null) {
                return temporaryInstance
            }

            // This is a synchronization statement
            // It receives an object and a block of code to be executed
            // Everytime a thread reaches this point, the class is blocked
            // and code block is executed. So the instance of the database is
            // set by only one thread.
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    // Application wide-constant
                    context.applicationContext,
                    PodPlayDatabase::class.java,
                    "PodPlayer")
                    .build()

                INSTANCE = instance

                return instance
            }
        }
    }
}