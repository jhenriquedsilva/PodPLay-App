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
         * Volatile means that assignments to these fields
         * are immediately made visible to other threads
         */
        @Volatile
        private var INSTANCE: PodPlayDatabase? = null

        fun getInstance(context: Context, coroutineScopde: CoroutineScope): PodPlayDatabase {
            val temporaryInstance = INSTANCE
            if (temporaryInstance!= null) {
                return temporaryInstance
            }

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