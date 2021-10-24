package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.RssFeedService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Retrieves the podcast details and returns them to the view model
class PodcastRepo {

    // I added the suspend keyword, but the original is not suspended
    fun getPodcast(feedUrl: String): Podcast? {

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

    }
}