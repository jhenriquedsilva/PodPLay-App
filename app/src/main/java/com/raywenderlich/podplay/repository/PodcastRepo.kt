package com.raywenderlich.podplay.repository

import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


// Retrieves the podcast details and returns them to the view model
class PodcastRepo(
    private var rssFeedService: RssFeedService,
    private var podcastDao: PodcastDao
    ) {

    // Holds the updated details for a single podcast
    data class PodcastUpdateInfo(
        val feedUrl: String,
        val name: String,
        val newCount: Int
    )

    // Checks if there is new episodes and return them
    private suspend fun getNewEpisodes(localPodcast: Podcast): List<Episode> {
        val response = rssFeedService.getFeed(localPodcast.feedUrl)

        if (response != null) {
            val remotePodcast =
                rssFeedResponseToPodcast(localPodcast.feedUrl,
                localPodcast.imageUrl, response)
            remotePodcast?.let { remotePodcast ->
                val localEpisodes =
                    podcastDao.loadEpisodes(localPodcast.id as Long)

                // Creates a list with the new episodes only
                return remotePodcast.episodes.filter { remoteEpisode ->
                    localEpisodes.find { localEpisode -> remoteEpisode.guid == localEpisode.guid } == null
                }
            }
        }
        return listOf()
    }

    // Save the new episodes in the database
    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    // That's the podcast update method
    suspend fun updatePodcastEpisodes(): MutableList<PodcastUpdateInfo> {
        val updatedPodcasts: MutableList<PodcastUpdateInfo> =
            mutableListOf()

        val podcasts = podcastDao.loadPodcastsStatic()

        for (podcast in podcasts) {
            val newEpisodes = getNewEpisodes(podcast)

            if (newEpisodes.count() > 0) {
                podcast.id?.let { id ->
                    saveNewEpisodes(id,newEpisodes)
                    updatedPodcasts.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl,
                            podcast.feedTitle,
                            newEpisodes.count()
                        )
                    )
                }
            }

        }

        return updatedPodcasts
    }

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

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
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
        // This piece first checks the local db
        val podcastLocal = podcastDao.loadPodcast(feedUrl)
        if (podcastLocal != null) {
            podcastLocal.id?.let { id ->
                podcastLocal.episodes = podcastDao.loadEpisodes(id)
                return podcastLocal
            }
        }
        // This piece fetches the internet
        var podcast: Podcast? = null
        // This property is in the class constructor
        val rssFeedResponse = rssFeedService.getFeed(feedUrl)

        if (rssFeedResponse != null) {
            podcast = rssFeedResponseToPodcast(feedUrl, "", rssFeedResponse)
        }
        return podcast
    }
}