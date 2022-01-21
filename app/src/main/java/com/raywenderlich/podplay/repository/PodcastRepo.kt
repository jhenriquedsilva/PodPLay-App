package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Retrieves the podcast details and returns them to the view model
class PodcastRepo(private var feedService: FeedService) {

    // Convert RssResponse data into Episode and Podcast objects
    private fun rssItemsToEpisodes(
        episodeResponses: List<RssFeedResponse.EpisodeResponse>
    ): List<Episode> {
        return episodeResponses.map { episodeResponse ->
            Episode(
                episodeResponse.guid ?: "",
                episodeResponse.title ?: "",
                episodeResponse.description ?: "",
                episodeResponse.url ?: "",
                episodeResponse.type ?: "",
                DateUtils.xmlDateToDate(episodeResponse.pubDate),
                episodeResponse.duration ?: ""
            )
        }
    }

    // Convert RssFeedResponse to a Podcast
    private fun rssResponseToPodcast(
        feedUrl: String, imageUrl: String, rssResponse: RssFeedResponse
    ): Podcast? {
        // If it is null, stop the method
        // There was an elvis operator over here.
        // But i removed it because it was useless
        // However if there is a bug, check if the problem
        // occurs over here
        val items = rssResponse.episodes ?: return null

        val description = if (rssResponse.description == "") rssResponse.summary else (rssResponse.description)

        return Podcast(
            feedUrl,
            rssResponse.title,
            description,
            imageUrl,
            rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )

    }

    // I added the suspend keyword, but the original is not suspended
    fun getPodcast(feedUrl: String): Podcast? {

        var podcast: Podcast? = null

        // This property is in the class constructor
        val feedResponse = feedService.getFeed(feedUrl)
        if (feedResponse != null) {
            podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
        }
        return podcast

        /*
        val rssFeedService = RssFeedService.instance
        GlobalScope.launch {
            rssFeedService.getFeed(feedUrl)
        }
        return Podcast(
            feedUrl,
            "No name",
            "No description",
            "No image"
        )
        */
    }
}