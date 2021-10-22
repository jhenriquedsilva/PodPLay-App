package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.model.Podcast

// Retrieves the podcast details and returns them to the view model
class PodcastRepo {

    fun getPodcast(feedUrl: String): Podcast? {
        return Podcast(
            feedUrl,
            "No name",
            "No description",
            "No image"
        )
    }
}