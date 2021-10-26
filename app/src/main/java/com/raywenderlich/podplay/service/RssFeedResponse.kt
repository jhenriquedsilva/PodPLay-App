package com.raywenderlich.podplay.service

import java.util.Date

// Holds the parsed RSS feed response

data class RssFeedResponse(
    var title : String = "",
    var description : String = "",
    var summary : String = "",
    var lastUpdated : Date = Date(),
    var episodes: MutableList<EpisodeResponse>
    ) {
    data class EpisodeResponse(
        var title: String? = null,
        var link: String? = null, // URL link to the episode media file
        var description: String? = null, // Episode description
        var guid: String? = null, // Unique ID for the episode
        var pubDate: String? = null, // Publication date of the episode
        var duration: String? = null,
        var url: String? = null, // URL to the episode landing page
        var type: String? = null // Type of media for the episode 'audio or video'
    )
}
