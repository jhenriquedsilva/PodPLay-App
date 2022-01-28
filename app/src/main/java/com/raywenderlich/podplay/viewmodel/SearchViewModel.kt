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


        if (results != null && results.isSuccessful) {
            // Results is only the list
            val podcasts = results.body()?.results
            // Check if the podcasts list is not empty
            // Returns true if the list is either null or empty
            if (!podcasts.isNullOrEmpty()) {
                // The list has to be values to be mapped
                // Convert each raw podcast to a view podcast
                val itunesPodcastList = podcasts.map { podcast ->
                    itunesPodcastToPodcastSummaryView(podcast)
                }

                return itunesPodcastList
            }
        }

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