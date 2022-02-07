package com.raywenderlich.podplay.repository

import android.util.Log
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService



// Retrieves the podcast details and returns them to the view model
class PodcastRepo(private var rssFeedService: RssFeedService) {

    // Convert RssResponse data into Episode and Podcast objects
    private fun rssEpisodesResponseToEpisodes(
        episodeResponses: List<RssFeedResponse.EpisodeResponse>
    ): List<Episode> {
        val listOfEpisodes = episodeResponses.map { episodeResponse ->
            Episode(
                episodeResponse.guid ?: "",
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