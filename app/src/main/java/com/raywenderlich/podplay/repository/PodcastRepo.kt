package com.raywenderlich.podplay.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


// Retrieves the podcast details and returns them to the view model
class PodcastRepo(
    private var rssFeedService: RssFeedService,
    private var podcastDao: PodcastDao
    ) {

    // This method should be asynchronous.
    // So it should be run in a coroutine scope
    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    // Saves a podcast to the database
    // Database access should occur in the background
    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)

            for(episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    // Convert RssResponse data into Episode and Podcast objects
    private fun rssEpisodesResponseToEpisodes(
        episodeResponses: List<RssFeedResponse.EpisodeResponse>
    ): List<Episode> {
        val listOfEpisodes = episodeResponses.map { episodeResponse ->
            Episode(
                episodeResponse.guid ?: "",
                null,
                episodeResponse.title ?: "",
                episodeResponse.description ?: "",
                episodeResponse.url ?: "",
                episodeResponse.type ?: "",
                episodeResponse.pubDate ?: "",
                episodeResponse.duration ?: ""
            )
        }
        return listOfEpisodes
    }

    // Convert RssFeedResponse to a Podcast
    private fun rssFeedResponseToPodcast(
        feedUrl: String, imageUrl: String, rssFeedResponse: RssFeedResponse
    ): Podcast? {
        // If it is null, stop the method
        // There was an elvis operator over here.
        // But i removed it because it was useless
        // However if there is a bug, check if the problem
        // occurs over here
        val episodesResponse = rssFeedResponse.episodes ?: return null

        val description: String
        if (rssFeedResponse.description == "") {
            description = rssFeedResponse.summary
        } else {
            description = rssFeedResponse.description
        }

        return Podcast(
            null,
            feedUrl,
            rssFeedResponse.title,
            description,
            imageUrl,
            rssFeedResponse.lastUpdated,
            rssEpisodesResponseToEpisodes(episodesResponse)
        )

    }

    // Retrieves the feed from the URL and parse it into a Podcast object
    suspend fun getPodcast(feedUrl: String): Podcast? {

        var podcast: Podcast? = null
        // This property is in the class constructor
        val rssFeedResponse = rssFeedService.getFeed(feedUrl)
        Log.d("PodcastRepo","Value of $rssFeedResponse")
        if (rssFeedResponse != null) {
            podcast = rssFeedResponseToPodcast(feedUrl, "", rssFeedResponse)
            Log.d("PodcastRepo","Value of $podcast")
        }
        return podcast
    }
}