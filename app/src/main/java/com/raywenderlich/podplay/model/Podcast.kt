package com.raywenderlich.podplay.model

import java.util.Date

// Retrieves the podcast details and return it to the view model
data class Podcast(
    var feedUrl: String = "", // Location of the RSS feed
    var feedTitle: String = "",
    var feedDesc: String = "",
    var imageUrl: String = "",
    var lastUpdated: Date = Date(),
    var episodes: List<Episode> = listOf()
)