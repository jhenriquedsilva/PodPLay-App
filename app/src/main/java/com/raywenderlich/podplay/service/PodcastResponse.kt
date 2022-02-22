package com.raywenderlich.podplay.service

/**
 * This class holds the data for each Podcast item
 */

/**
 * This is the body() of the Response object
 */
data class PodcastResponse(
    val resultCount: Int,
    val results: List<ItunesPodcast>
)
{
    /**
     * This class indicates which attributes will be
     * used from the podcast response
     */
    data class ItunesPodcast(
        val collectionCensoredName: String,
        val feedUrl: String,
        val artworkUrl100: String,
        val releaseDate: String
    )
}
