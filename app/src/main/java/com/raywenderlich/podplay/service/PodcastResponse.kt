package com.raywenderlich.podplay.service

// This class handles the response from the iTunes service
// So it is good to keep it over here
data class PodcastResponse(
    val resultCount: Int,
    val results: List<ItunesPodcast>
)
{
    data class ItunesPodcast(
        val collectionCensoredName: String,
        val feedUrl: String,
        val artworkUrl30: String,
        val releaseDate: String
    )
}
