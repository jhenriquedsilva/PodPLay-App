package com.raywenderlich.podplay.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import kotlinx.coroutines.CoroutineScope

@Database(entities = [Podcast::class, Episode::class], version = 1)
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
        fun getInstance(context: Context, coroutineScopde: CoroutineScope): PodPlayDatabase {

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