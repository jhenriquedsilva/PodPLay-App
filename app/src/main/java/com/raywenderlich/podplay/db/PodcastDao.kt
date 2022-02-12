package com.raywenderlich.podplay.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast

@Dao
interface PodcastDao {

    /**
     * Coroutines are built into Room so all functions run
     * in the background even though you write them in a
     * synchronous fashion
     */

    @Query("SELECT * FROM podcasts WHERE feedUrl = :url")
    suspend fun loadPodcast(url: String): Podcast?

    // It's not necessary to add the suspend keyword in this
    // function because LiveData already uses the suspend keyword.
    // If you use it, the app will not compile
    @Query("SELECT * FROM podcasts ORDER BY feedTitle")
    fun loadPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    suspend fun loadEpisodes(podcastId: Long): List<Episode>

    // Return the new id for that row
    @Insert(onConflict = REPLACE)
    suspend fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertEpisode(episode:Episode): Long

    @Delete
    suspend fun deletePodcast(podcast: Podcast)
}