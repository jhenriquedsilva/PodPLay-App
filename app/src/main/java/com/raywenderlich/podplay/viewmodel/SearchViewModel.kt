package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.service.PodcastResponse
import com.raywenderlich.podplay.util.DateUtils

class SearchViewModel(application: Application): AndroidViewModel(application) {

    // PodcastActivity passes this object in before calling any method to fetch the data
    var iTunesRepo: ItunesRepo? = null

    // Perform the search
    // Since the iTunesRepo's search method runs asynchronously,
    // this method needs the suspend keyword
    suspend fun searchPodcasts(term: String): List<PodcastSummaryViewData> {

        val results = iTunesRepo?.searchByTerm(term)

        // 3
        if (results != null && results.isSuccessful) {
            // Body returns the objects
            val podcasts = results.body()?.results
            // Check if the podcasts list is not empty
            // Returns true if the list is either null or empty
            if (!podcasts.isNullOrEmpty()) {
                // Convert each raw podcast to a view podcast
                return podcasts.map { podcast ->
                    itunesPodcastToPodcastSummaryView(podcast)
                }
            }
        }
        // 7
        return emptyList()
    }


    // Method to convert from the raw model data to the view data
    private fun itunesPodcastToPodcastSummaryView(
        itunesPodcast: PodcastResponse.ItunesPodcast
    ): PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl
        )
    }

    // This class has only data that's necessary for the View
    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )
}